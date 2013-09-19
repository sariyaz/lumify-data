package com.altamiracorp.lumify.core.user;

public class SystemUser extends User {
    private static final String ROW_KEY = "system";
    private static final String USERNAME = "system";
    private static final String CURRENT_WORKSPACE = null;

    public SystemUser() {
        super(ROW_KEY, USERNAME, CURRENT_WORKSPACE, getSystemModelAuthorizations());
    }

    public static ModelAuthorizations getSystemModelAuthorizations() {
        // TODO: figure out a better way to create this
        String className = "com.altamiracorp.lumify.model.AccumuloModelAuthorizations";
        try {
            return (ModelAuthorizations) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create: " + className);
        }
    }
}
