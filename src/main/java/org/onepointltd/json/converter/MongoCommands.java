package org.onepointltd.json.converter;

import static org.onepointltd.json.converter.TextUtil.hasText;

/**
 * Contains the Mongo update command content.
 */
public class MongoCommands {

    private final String unset;

    private final String pull;

    private final String set;

    private final String push;

    MongoCommands(String unset, String pull, String set, String push) {
        this.unset = unset;
        this.pull = pull;
        this.set = set;
        this.push = push;
    }

    public String getUnset() {
        return unset;
    }

    public String getSet() {
        return set;
    }

    public String getPush() {
        return push;
    }

    public String getPull() {
        return pull;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("");
        String lb = String.format("%n");
        sb.append("unset=").append(unset).append(lb);
        sb.append("pull=").append(unset).append(lb);
        sb.append("set=").append(set).append(lb);
        sb.append("push=").append(push).append(lb);
        return sb.toString();
    }


    public String asJavascript(boolean inFunction) {
        final StringBuilder sb = new StringBuilder("");
        String lineSep = System.getProperty("line.separator");
        String lb = ";" + lineSep;
        if(inFunction) {
            sb.append("function update() {").append(lineSep);
        }
        if(hasText(unset)) {
            sb.append("    ").append(unset).append(lb);
        }
        if(hasText(pull)) {
            sb.append("    ").append(pull).append(lb);
        }
        if(hasText(set)) {
            sb.append("    ").append(set).append(lb);
        }
        if(hasText(push)) {
            sb.append("    ").append(push).append(lb);
        }
        if(inFunction) {
            sb.append("}").append(lineSep);
        }
        return sb.toString();
    }
}
