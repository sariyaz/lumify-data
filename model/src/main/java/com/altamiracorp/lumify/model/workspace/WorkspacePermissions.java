package com.altamiracorp.lumify.model.workspace;

import com.altamiracorp.lumify.model.ColumnFamily;
import com.altamiracorp.lumify.model.Value;
import org.json.JSONObject;

/**
 * This column family stores the user's rowkey as the column name and their permissions
 * as the column value for a particular workspace.
 */

public class WorkspacePermissions extends ColumnFamily {
    public static final String NAME = "users";
    public static final String USER = "user";

    public WorkspacePermissions() {
        super(NAME);
    }

    public String getUsers () {
        return Value.toString(get(USER));
    }

    public WorkspacePermissions setPermissions (String columnName, Object value) {
        this.set(columnName, value);
        return this;
    }
}
