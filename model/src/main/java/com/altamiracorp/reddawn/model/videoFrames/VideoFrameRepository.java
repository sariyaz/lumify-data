package com.altamiracorp.reddawn.model.videoFrames;

import com.altamiracorp.reddawn.model.*;
import com.altamiracorp.reddawn.ucd.artifact.ArtifactRowKey;

import java.io.InputStream;
import java.util.Collection;

public class VideoFrameRepository extends Repository<VideoFrame> {
    @Override
    public VideoFrame fromRow(Row row) {
        VideoFrame videoFrame = new VideoFrame(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(VideoFrameMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                videoFrame.addColumnFamily(new VideoFrameMetadata().addColumns(columns));
            } else {
                videoFrame.addColumnFamily(columnFamily);
            }
        }
        return videoFrame;
    }

    @Override
    public Row toRow(VideoFrame videoFrame) {
        return videoFrame;
    }

    @Override
    public String getTableName() {
        return VideoFrame.TABLE_NAME;
    }

    public void saveVideoFrame(Session session, ArtifactRowKey artifactRowKey, InputStream in, long frameStartTime) {
        SaveFileResults saveFileResults = session.saveFile(in);
        VideoFrameRowKey videoFrameRowKey = new VideoFrameRowKey(artifactRowKey.toString(), frameStartTime);
        VideoFrame videoFrame = new VideoFrame(videoFrameRowKey);
        videoFrame.getMetadata()
                .setHdfsPath(saveFileResults.getFullPath());
        this.save(session, videoFrame);
    }
}