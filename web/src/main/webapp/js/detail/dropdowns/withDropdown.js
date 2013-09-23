
define([], function() {
    'use strict';

    function withDropdown() {

        this.open = function() {
            var self = this,
                node = this.$node;

            if (node.outerWidth() <= 0) {
                // Fix issue where dropdown is zero width/height 
                // when opening dropdown later in detail pane when
                // dropdown is already open earlier in detail pane
                node.css({position:'relative'});
                return _.defer(this.open.bind(this));
            }

            node.one('transitionend webkitTransitionEnd oTransitionEnd otransitionend', function() {
                node.off('transitionend webkitTransitionEnd oTransitionEnd otransitionend');
                node.css({
                    transition: 'none',
                    height: 'auto',
                    width: '100%',
                    overflow: 'visible'
                });
                self.trigger('opened');
            });
            var form = node.find('.form');
            node.css({ height:form.outerHeight(true) + 'px' });
        };

        this.after('teardown', function() {
            this.$node.closest('.text').removeClass('dropdown');

            this.$node.remove();
        });

        this.after('initialize', function() {
            this.$node.closest('.text').addClass('dropdown');
            _.defer(this.open.bind(this));
        });
    }

    return withDropdown;
});

