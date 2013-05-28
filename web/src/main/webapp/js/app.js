
define([
    'tpl!app',
    'flight/lib/component',
    'search/search',
    'graph/graph',
    'detail/detail'
], function(appTemplate, defineComponent, Search, Graph, Detail) {
    'use strict';

    return defineComponent(App);

    function App() {

        this.onError = function(evt, err) {
            alert("Error: " + err.message); // TODO better error handling
        };

        this.after('initialize', function() {
            this.on(document, 'error', this.onError);

            var content = $(appTemplate({})),
                searchPane = content.filter('.search-pane'),
                graphPane = content.filter('.graph-pane'),
                detailPane = content.filter('.detail-pane');

            Search.attachTo(searchPane);
            Graph.attachTo(graphPane);
            Detail.attachTo(detailPane);

            // Configure splitpane resizing
            resizable(searchPane, 'e');
            resizable(detailPane, 'w');

            this.$node.html(content);
        });

    }


    function resizable( el, handles ) {
        return el.resizable({
            handles: handles,
               minWidth: 150,
               maxWidth: 300
        });
    }

});


