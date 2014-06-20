package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * Created by jon.hellmann on 6/20/14.
 *
 * NOTE: Checks ICC and XMP. May need to check others later.
 *
 */
public class MakeExtractor {

    /**
     * Checks the metadata directories in order until the make is found. The first match is returned.
     * @param metadata
     * @return
     */
    public static String getMake(Metadata metadata){


        String makeString = null;

        ExifIFD0Directory exifDir = metadata.getDirectory(ExifIFD0Directory.class);
        if (exifDir != null) {
            makeString = exifDir.getDescription(ExifIFD0Directory.TAG_MAKE);
            if (makeString != null && !makeString.equals("none")) {
                return makeString;
            }
        }

        IccDirectory iccDir = metadata.getDirectory(IccDirectory.class);
        if (iccDir != null) {
            makeString = iccDir.getDescription(IccDirectory.TAG_ICC_DEVICE_MAKE);
            if (makeString != null && !makeString.equals("none")) {
                return makeString;
            }
        }

        XmpDirectory xmpDir = metadata.getDirectory(XmpDirectory.class);
        if (makeString != null && !makeString.equals("none")) {
            makeString = xmpDir.getDescription(XmpDirectory.TAG_MAKE);
            if (makeString != null) {
                return makeString;
            }
        }

        return null;

    }
}
