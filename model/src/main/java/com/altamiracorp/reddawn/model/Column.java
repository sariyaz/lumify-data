package com.altamiracorp.reddawn.model;

public class Column {
    private final String name;
    private final Value value;

    public Column(String name, Object value) {
        this.name = name;
        this.value = new Value(value);
    }

    public String getName() {
        return name;
    }

    public Value getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toString(result, "");
        return result.toString();
    }

    public void toString(StringBuilder out, String indent) {
        out.append(indent + getName() + ": " + getValue() + "\n");
    }
}
