package com.altamiracorp.lumify.storm.video;

import com.altamiracorp.lumify.core.ingest.AdditionalArtifactWorkData;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.TextExtractionWorkerPrepareData;
import com.altamiracorp.lumify.core.ingest.video.VideoTextExtractionWorker;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.ProcessRunner;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;
import com.google.inject.Inject;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

public class VideoMp4EncodingWorker extends ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalArtifactWorkData> implements VideoTextExtractionWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoMp4EncodingWorker.class);
    private static final Random random = new Random();
    private ProcessRunner processRunner;

    @Override
    public void prepare(TextExtractionWorkerPrepareData data) {
    }

    @Override
    protected ArtifactExtractedInfo doWork(InputStream work, AdditionalArtifactWorkData data) throws Exception {
        LOGGER.info("Encoding (mp4) [VideoMp4EncodingWorker] %s", data.getFileName());
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        File mp4ReloactedFile = File.createTempFile("relocated_mp4_", ".mp4");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFileName(),
                            "-vcodec", "libx264",
                            "-vprofile", "high",
                            "-preset", "slow",
                            "-b:v", "500k",
                            "-maxrate", "500k",
                            "-bufsize", "1000k",
                            "-vf", "scale=720:480",
                            "-threads", "0",
                            "-acodec", "libfdk_aac",
                            "-b:a", "128k",
                            "-f", "mp4",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getFileName() + ": "
            );

            processRunner.execute(
                    "qt-faststart",
                    new String[]{
                            mp4File.getAbsolutePath(),
                            mp4ReloactedFile.getAbsolutePath()
                    },
                    null,
                    data.getFileName() + ": "
            );

            Path copySourcePath = new Path(mp4ReloactedFile.getAbsolutePath());
            Path copyDestPath = new Path("/tmp/videoMp4-" + random.nextInt() + ".mp4");
            data.getHdfsFileSystem().copyFromLocalFile(false, true, copySourcePath, copyDestPath);

            ArtifactExtractedInfo info = new ArtifactExtractedInfo();
            info.setMp4HdfsFilePath(copyDestPath.toString());
            LOGGER.debug("Finished [VideoMp4EncodingWorker]: %s", data.getFileName());
            return info;
        } finally {
            mp4File.delete();
            mp4ReloactedFile.delete();
        }
    }

    @Inject
    public void setProcessRunner(ProcessRunner ffmpeg) {
        this.processRunner = ffmpeg;
    }
}