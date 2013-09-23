
define([
    'flight/lib/component',
    'data',
    './image/image',
    '../properties',
    '../withTypeContent',
    '../withHighlighting',
    'tpl!./entity',
    'tpl!./relationships',
    'util/vertexList/list',
    'detail/dropdowns/propertyForm/propForm',
    'service/ontology',
    'service/vertex',
    'sf'
], function(defineComponent, 
    appData,
    Image,
    Properties,
    withTypeContent,
    withHighlighting,
    template,
    relationshipsTemplate,
    VertexList,
    PropertyForm,
    OntologyService,
    VertexService,
    sf) {
    'use strict';

    var ontologyService = new OntologyService();
    var vertexService = new VertexService();

    return defineComponent(Entity, withTypeContent, withHighlighting);

    function Entity(withDropdown) {

        this.defaultAttrs({
            glyphIconSelector: '.entity-glyphIcon',
            propertiesSelector: '.properties',
            relationshipsSelector: '.relationships',
            titleSelector: '.entity-title'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));

            this.handleCancelling(ontologyService.concepts(function(err, concepts) {
                if (err) {
                    console.error('handleCancelling', err);
                    return self.trigger(document, 'error', err);
                }


                self.loadEntity();
            }));
        });

        this.loadEntity = function() {
            var self = this;

            $.when( 
                appData.refresh(this.attr.data),
                this.handleCancelling(ontologyService.concepts())
            ).done(function(vertex, concepts) {
                var concept = concepts.byId[self.attr.data._subType];

                self.$node.html(template({
                    title: self.attr.data.properties.title,
                    highlightButton: self.highlightButton(),
                    fullscreenButton: self.fullscreenButton([self.attr.data.id])
                }));

                Image.attachTo(self.select('glyphIconSelector'), {
                    data: self.attr.data,
                    service: self.entityService,
                    defaultIconSrc: concept && concept.glyphIconHref || ''
                });

                Properties.attachTo(self.select('propertiesSelector'), {
                    data: self.attr.data
                });

                $.when(
                    self.handleCancelling(self.ontologyService.relationships()),
                    self.handleCancelling(self.ucdService.getVertexRelationships(self.attr.data.id))
                ).done(self.loadRelationships.bind(self, vertex));
            });
        };

        this.loadRelationships = function(vertex, ontologyRelationships, vertexRelationships) {
            var self = this,
                relationships = vertexRelationships[0].relationships;

            // Create source/dest/other properties
            relationships.forEach(function(r) {
                var src, dest, other;
                if (vertex.id == r.relationship.sourceVertexId) {
                    src = vertex;
                    dest = other = r.vertex;
                } else {
                    src = other = r.vertex;
                    dest = vertex;
                }

                r.vertices = {
                    src: src,
                    dest: dest,
                    other: other,
                    classes: {
                        src: self.classesForVertex(src),
                        dest: self.classesForVertex(dest),
                        other: self.classesForVertex(other)
                    }
                };
                r.dataInfo = {
                    source: src.id,
                    target: dest.id,
                    _type: 'relationship',
                    relationshipType: r.relationship.label
                };
                r.displayLabel = ontologyRelationships.byTitle[r.relationship.label].displayName;
            });

            var groupedByType = _.groupBy(relationships, function(r) { 

                // Has Entity are collected into references (no matter
                // relationship direction
                if (r.relationship.label === 'hasEntity') {
                    return 'references';
                }

                // Group all that are relations from this vertex (not dest)
                if (r.relationship.sourceVertexId === vertex.id) {
                    return r.displayLabel;
                }

                // Collect all relationships that are destined here
                // into section
                return 'inverse';
            });
            var sortedKeys = Object.keys(groupedByType);
            sortedKeys.sort(function(a,b) {
                // If in inverse group, sort by the type
                if (a === b && a === 'inverse') {
                    return a.displayLabel === b.displayLabel ? 0 : a.displayLabel < b.displayLabel ? -1 : 1;
                }

                // If in references group sort by the title
                if (a === b && a === 'references') {
                    return defaultSort(a.vertex.properties.title, b.vertex.properties.title);
                }

                // Specifies the special group sort order
                var groups = { inverse:1, references:2 };
                if (groups[a] && groups[b]) {
                    return defaultSort(groups[a], groups[b]);
                } else if (groups[a]) {
                    return 1;
                } else if (groups[b]) {
                    return -1;
                }

                return defaultSort(a, b);

                function defaultSort(x,y) {
                    return x === y ? 0 : x < y ? -1 : 1;
                }
            });

            var $rels = self.select('relationshipsSelector');
            $rels.html(relationshipsTemplate({
                relationshipsGroupedByType: groupedByType,
                sortedKeys: sortedKeys
            }));

            VertexList.attachTo($rels.find('.references'), {
                vertices: _.map(groupedByType.references, function(r) {
                    return r.vertices.other;
                })
            });
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if (!$target.is('.add-new-properties') && $target.parents('.underneath').length === 0) {
                PropertyForm.teardownAll();
            }

            if ($target.is('.entity, .artifact, span.relationship')) {
                this.trigger('verticesSelected', $target.data('info'));
                evt.stopPropagation();
            }

        };

    }
});

