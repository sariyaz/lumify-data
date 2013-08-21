define([
    'flight/lib/component',
    '../withTypeContent',
    '../withHighlighting',
    'tpl!./relationship',
    'underscore'
], function(defineComponent, withTypeContent, withHighlighting, template, _) {

    'use strict';

    return defineComponent(Relationship, withTypeContent, withHighlighting);

    function Relationship() {

        this.defaultAttrs({
            vertexToVertexRelationshipSelector: '.vertex-to-vertex-relationship',
        });

        this.after('initialize', function() {

            this.on('click', {
                vertexToVertexRelationshipSelector: this.onVertexToVertexRelationshipClicked
            });

            var data = this.attr.data;
            this.loadRelationship ();
        });


        this.loadRelationship = function() {
            var self = this,
                data = this.attr.data;

            this.ucdService.getVertexToVertexRelationshipDetails (data.source, data.target, data.relationshipType, function (err, relationshipData){
                if (err) {
                    console.error ('Error', err);
                    return self.trigger (document, 'error', { message: err.toString () });
                }

                self.$node.html (template({
                    highlightButton: self.highlightButton(),
                    relationshipData: relationshipData
                }));
                self.updateEntityAndArtifactDraggables();
            });
        };

        this.onVertexToVertexRelationshipClicked = function(evt) {
            var self = this;
            var $target = $(evt.target);

            this.trigger (document, 'searchResultSelected',  $target.data('info'));
        };
    }
});











