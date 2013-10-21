package com.altamiracorp.lumify.model.artifactThumbnails;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.model.Column;
import com.altamiracorp.lumify.model.ColumnFamily;
import com.altamiracorp.lumify.model.ModelSession;
import com.altamiracorp.lumify.model.Repository;
import com.altamiracorp.lumify.model.Row;
import com.altamiracorp.lumify.ucd.artifact.ArtifactRowKey;
import com.google.inject.Inject;

public class ArtifactThumbnailRepository extends Repository<ArtifactThumbnail> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactThumbnailRepository.class);

    @Inject
    public ArtifactThumbnailRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public ArtifactThumbnail fromRow(Row row) {
        ArtifactThumbnail artifactThumbnail = new ArtifactThumbnail(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(ArtifactThumbnailMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                artifactThumbnail.addColumnFamily(new ArtifactThumbnailMetadata().addColumns(columns));
            } else {
                artifactThumbnail.addColumnFamily(columnFamily);
            }
        }
        return artifactThumbnail;
    }

    @Override
    public Row toRow(ArtifactThumbnail artifactThumbnail) {
        return artifactThumbnail;
    }

    @Override
    public String getTableName() {
        return ArtifactThumbnail.TABLE_NAME;
    }

    public ArtifactThumbnail getThumbnail(ArtifactRowKey artifactRowKey, String thumbnailType, int width, int height, User user) {
        ArtifactThumbnailRowKey rowKey = new ArtifactThumbnailRowKey(artifactRowKey.toString(), thumbnailType, width, height);
        return findByRowKey(rowKey.toString(), user);
    }

    public byte[] getThumbnailData(ArtifactRowKey artifactRowKey, String thumbnailType, int width, int height, User user) {
        ArtifactThumbnail artifactThumbnail = getThumbnail(artifactRowKey, thumbnailType, width, height, user);
        if (artifactThumbnail == null) {
            return null;
        }
        return artifactThumbnail.getMetadata().getData();
    }

    public ArtifactThumbnail createThumbnail(ArtifactRowKey artifactRowKey, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException {
        BufferedImage originalImage = ImageIO.read(in);
        int[] originalImageDims = new int[]{originalImage.getWidth(), originalImage.getHeight()};
        int[] newImageDims = getScaledDimension(originalImageDims, boundaryDims);

        if (newImageDims[0] >= originalImageDims[0] || newImageDims[1] >= originalImageDims[1]) {
            LOGGER.info("Original image dimensions " + originalImageDims[0] + "x" + originalImageDims[1] + " are smaller "
                    + "than requested dimensions " + newImageDims[0] + "x" + newImageDims[1]
                    + " returning original.");
        }

        int type = thumnbailType(originalImage);
        String format = thumbnailFormat(originalImage);

        BufferedImage resizedImage = new BufferedImage(newImageDims[0], newImageDims[1], type);
        Graphics2D g = resizedImage.createGraphics();
        if (originalImage.getColorModel().getNumComponents() > 3) {
            g.drawImage(originalImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
        } else {
            g.drawImage(originalImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), Color.BLACK, null);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, out);

        return saveThumbnail(artifactRowKey, thumbnailType, boundaryDims, out.toByteArray(), type, format, user);
    }

    public int thumnbailType(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return BufferedImage.TYPE_4BYTE_ABGR;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    public String thumbnailFormat(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return "png";
        }
        return "jpg";
    }

    private ArtifactThumbnail saveThumbnail(ArtifactRowKey artifactRowKey, String thumbnailType, int[] boundaryDims, byte[] bytes, int type, String format, User user) {
        ArtifactThumbnailRowKey artifactThumbnailRowKey = new ArtifactThumbnailRowKey(artifactRowKey.toString(), thumbnailType, boundaryDims[0], boundaryDims[1]);
        ArtifactThumbnail artifactThumbnail = new ArtifactThumbnail(artifactThumbnailRowKey);
        artifactThumbnail.getMetadata().setData(bytes);
        artifactThumbnail.getMetadata().setType(type);
        artifactThumbnail.getMetadata().setFormat(format);
        save(artifactThumbnail, user);
        return artifactThumbnail;
    }

    public static int[] getScaledDimension(int[] imgSize, int[] boundary) {
        int originalWidth = imgSize[0];
        int originalHeight = imgSize[1];
        int boundWidth = boundary[0];
        int boundHeight = boundary[1];
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > boundWidth) {
            newWidth = boundWidth;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        if (newHeight > boundHeight) {
            newHeight = boundHeight;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new int[]{newWidth, newHeight};
    }
}