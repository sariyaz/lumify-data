package com.altamiracorp.lumify.twitter.storm;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

/**
 * This Bolt accepts an input Tuple containing a JSON-serialized Twitter
 * <a href="https://dev.twitter.com/docs/platform-objects/tweets">Tweet</a>
 * object in the <code>json</code> field and the Lumify GraphVertex representing
 * the Tweet artifact in the <code>tweetVertex</code> field.  It executes the
 finalizeTweetVertex() method of the LumifyTwitterProcessor to run any final tasks
 on the fully processed Tweet.
 
 <h2>Input Tuple:</h2>
 * <table>
 * <tr><th>Field</th><th>Type</th><th>Value</th></tr>
 * <tr><td>json</td><td>String</td><td>serialized Tweet JSON object</td></tr>
 * <tr><td>tweetVertex</td><td>GraphVertex</td><td>the Lumify GraphVertex for the Tweet artifact</td></tr>
 * </table>
 */
public class TweetFinalizerBolt extends BaseTwitterBolt {
    @Override
    protected void processJson(final JSONObject json, final Tuple input) throws Exception {
        Vertex tweetVertex = (Vertex) input.getValueByField(TwitterStormConstants.TWEET_VERTEX_FIELD);
        if (tweetVertex != null) {
            getTwitterProcessor().finalizeTweetVertex(getProcessId(), tweetVertex.getId().toString());
        }
    }

    @Override
    public void declareOutputFields(final OutputFieldsDeclarer ofd) {
    }
}
