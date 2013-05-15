package com.altamiracorp.reddawn.ucd.models;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;

import java.util.Map;

public class ArtifactDynamicMetadata {
  public static final String COLUMN_FAMILY_NAME = "Dynamic_Metadata";
  private String artifactSerialNum;
  private String docSourceHash;
  private String edhGuid;
  private String geoLocation;
  private String provenanceId;
  private String sourceHashAlgorithm;
  private String sourceLabel;

  private ArtifactDynamicMetadata() {

  }

  public String getArtifactSerialNum() {
    return artifactSerialNum;
  }

  public String getDocSourceHash() {
    return docSourceHash;
  }

  public String getEdhGuid() {
    return edhGuid;
  }

  public String getGeoLocation() {
    return geoLocation;
  }

  public String getProvenanceId() {
    return provenanceId;
  }

  public String getSourceHashAlgorithm() {
    return sourceHashAlgorithm;
  }

  public String getSourceLabel() {
    return sourceLabel;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  void addMutations(Mutation mutation) {
    // TODO
  }

  public static class Builder {
    private ArtifactDynamicMetadata artifactDynamicMetadata = new ArtifactDynamicMetadata();

    private Builder() {

    }

    public ArtifactDynamicMetadata build() {
      return this.artifactDynamicMetadata;
    }

    public void artifactSerialNum(String artifactSerialNum) {
      this.artifactDynamicMetadata.artifactSerialNum = artifactSerialNum;
    }

    public void docSourceHash(String docSourceHash) {
      this.artifactDynamicMetadata.docSourceHash = docSourceHash;
    }

    public void edhGuid(String edhGuid) {
      this.artifactDynamicMetadata.edhGuid = edhGuid;
    }

    public void geoLocation(String geoLocation) {
      this.artifactDynamicMetadata.geoLocation = geoLocation;
    }

    public void provenanceId(String provenanceId) {
      this.artifactDynamicMetadata.provenanceId = provenanceId;
    }

    public void sourceHashAlgorithm(String sourceHashAlgorithm) {
      this.artifactDynamicMetadata.sourceHashAlgorithm = sourceHashAlgorithm;
    }

    public void sourceLabel(String sourceLabel) {
      this.artifactDynamicMetadata.sourceLabel = sourceLabel;
    }

    public static void populateFromColumn(ArtifactDynamicMetadata dynamicMetadata, Map.Entry<Key, Value> column) {
      // TODO
    }
  }
}
