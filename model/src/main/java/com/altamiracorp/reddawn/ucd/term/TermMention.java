package com.altamiracorp.reddawn.ucd.term;

import com.altamiracorp.reddawn.model.ColumnFamily;
import com.altamiracorp.reddawn.model.GeoLocation;
import com.altamiracorp.reddawn.model.RowKeyHelper;
import com.altamiracorp.reddawn.model.Value;
import org.json.JSONException;
import org.json.JSONObject;

public class TermMention extends ColumnFamily {
    public static final String ARTIFACT_KEY = "artifactKey";
    public static final String ARTIFACT_KEY_SIGN = "artifactKey_sign";
    public static final String AUTHOR = "author";
    public static final String GEO_LOCATION = "geoLocation";
    public static final String MENTION = "mention";
    public static final String PROVENANCE_ID = "provenanceID";
    public static final String SECURITY_MARKING = "securityMarking";
    public static final String DATE = "date";

    public TermMention() {
        super(null);
    }

    public TermMention(String columnFamilyName) {
        super(columnFamilyName);
    }

    @Override
    public String getColumnFamilyName() {
        if (super.getColumnFamilyName() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getArtifactKey());
            sb.append(getMention());
            // TODO what else should be part of the hash?
            return RowKeyHelper.buildSHA256KeyString(sb.toString().getBytes());
        }
        return super.getColumnFamilyName();
    }

    public String getArtifactKey() {
        return Value.toString(get(ARTIFACT_KEY));
    }

    public TermMention setArtifactKey(String artifactKey) {
        set(ARTIFACT_KEY, artifactKey);
        return this;
    }

    public String getArtifactKeySign() {
        return Value.toString(get(ARTIFACT_KEY_SIGN));
    }

    public TermMention setArtifactKeySign(String artifactKeySign) {
        set(ARTIFACT_KEY_SIGN, artifactKeySign);
        return this;
    }

    public String getAuthor() {
        return Value.toString(get(AUTHOR));
    }

    public TermMention setAuthor(String author) {
        set(AUTHOR, author);
        return this;
    }

    public String getGeoLocation() {
        return Value.toString(get(GEO_LOCATION));
    }

    public TermMention setGeoLocation(String geoLocation) {
        set(GEO_LOCATION, geoLocation);
        return this;
    }

    public TermMention setGeoLocation(Double lat, Double lon) {
        return setGeoLocation(GeoLocation.getGeoLocation(lat, lon));
    }

    public String getMention() {
        return Value.toString(get(MENTION));
    }

    public TermMention setMention(String mention) {
        set(MENTION, mention);
        return this;
    }

    public TermMention setMention(JSONObject json) {
        setMention(json.toString());
        return this;
    }

    public JSONObject getMentionJSON() {
        try {
            String mention = getMention();
            if (mention == null) {
                return null;
            }
            return new JSONObject(mention);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Long getMentionStart() {
        JSONObject mentionJson = getMentionJSON();
        if (mentionJson == null) {
            return null;
        }
        try {
            return mentionJson.getLong("start");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public TermMention setMentionStart(Long start) {
        try {
            JSONObject mentionJson = getMentionJSON();
            if (mentionJson == null) {
                mentionJson = new JSONObject();
            }
            if (start != null) {
                mentionJson.put("start", start.longValue());
            } else {
                mentionJson.remove("start");
            }
            setMention(mentionJson);
            return this;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Long getMentionEnd() {
        JSONObject mentionJson = getMentionJSON();
        if (mentionJson == null) {
            return null;
        }
        try {
            return mentionJson.getLong("end");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public TermMention setMentionEnd(Long end) {
        try {
            JSONObject mentionJson = getMentionJSON();
            if (mentionJson == null) {
                mentionJson = new JSONObject();
            }
            if (end != null) {
                mentionJson.put("end", end.longValue());
            } else {
                mentionJson.remove("start");
            }
            setMention(mentionJson);
            return this;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getProvenanceId() {
        return Value.toString(get(PROVENANCE_ID));
    }

    public TermMention setProvenanceId(String provenanceId) {
        set(PROVENANCE_ID, provenanceId);
        return this;
    }

    public String getSecurityMarking() {
        return Value.toString(get(SECURITY_MARKING));
    }

    public TermMention setSecurityMarking(String securityMarking) {
        set(SECURITY_MARKING, securityMarking);
        return this;
    }

    public Long getDate() {
        return Value.toLong(get(DATE));
    }

    public TermMention setDate(Long date) {
        set(DATE, date);
        return this;
    }
}
