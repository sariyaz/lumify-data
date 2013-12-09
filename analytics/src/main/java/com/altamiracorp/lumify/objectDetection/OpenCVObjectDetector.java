package com.altamiracorp.lumify.objectDetection;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.objectDetection.ObjectDetector;
import com.altamiracorp.lumify.util.OpenCVUtils;
import com.google.inject.Inject;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OpenCVObjectDetector extends ObjectDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCVObjectDetector.class);

    private static final String MODEL = "opencv";

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (Exception ex) {
            String javaLibraryPath = System.getProperty("java.library.path");
            throw new RuntimeException("Could not find opencv library: " + Core.NATIVE_LIBRARY_NAME + " (java.library.path: " + javaLibraryPath + ")", ex);
        }
    }

    private CascadeClassifier objectClassifier;

    @Inject
    public OpenCVObjectDetector(ArtifactRepository artifactRepository, VideoFrameRepository videoFrameRepository) {
        super(artifactRepository, videoFrameRepository);
    }

    @Override
    public void setup(String classifierPath, InputStream dictionary) {
        LOGGER.warn("There is no dictionary needed for the standard OpenCV object detector, use OpenCVObjectDetector.setup(String) instead");
        setup(classifierPath);
    }

    @Override
    public void setup(String classifierPath) {
        objectClassifier = new CascadeClassifier(classifierPath);
    }

    @Override
    public List<ArtifactDetectedObject> detectObjects(BufferedImage bImage) {
        ArrayList<ArtifactDetectedObject> detectedObjectList = new ArrayList<ArtifactDetectedObject>();
        Mat image = OpenCVUtils.bufferedImageToMat(bImage);
        if (image != null) {
            MatOfRect faceDetections = new MatOfRect();
            objectClassifier.detectMultiScale(image, faceDetections);

            for (Rect rect : faceDetections.toArray()) {
                ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(Integer.toString(rect.x), Integer.toString(rect.y),
                        Integer.toString(rect.x + rect.width), Integer.toString(rect.y + rect.height));
                detectedObjectList.add(detectedObject);
            }
        }

        return detectedObjectList;
    }

    @Override
    public String getModelName() {
        return MODEL;
    }

}
