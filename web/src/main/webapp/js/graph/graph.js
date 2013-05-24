

define([
    'flight/lib/component',
    'tpl!./graph'
], function(defineComponent, template) {
    'use strict';

    return defineComponent(Graph);

    function Graph() {
        this.after('initialize', function() {

            this.$node.html(template({
                text: 'Graph pane'
            }));

        });
    }

});

