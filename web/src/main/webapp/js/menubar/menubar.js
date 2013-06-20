

define([
    'flight/lib/component',
    './activity/activity',
    'tpl!./menubar'
], function(defineComponent, Activity, template) {
    'use strict';

    return defineComponent(Menubar);

    function Menubar() {

        // Add class name of <li> buttons here
        var BUTTONS = 'graph map search workspaces activity users metrics prefs';

        // Which cannot both be active
        var MUTALLY_EXCLUSIVE_SWITCHES = [ 
            { names:['graph','map'], options: { allowCollapse:false } },
            { names:['workspaces', 'search'], options: { } }
        ];

        // Don't change state to highlighted on click
        var DISABLE_ACTIVE_SWITCH = 'activity metrics prefs'.split(' ');

        this.activities = 0;

        var attrs = {}, events = {};
        BUTTONS.split(' ').forEach(function(name) {
            var sel = name + 'IconSelector';

            attrs[sel] = '.' + name;
            events[sel] = function(e) {
                e.preventDefault();

                var isSwitch = false;
                if (DISABLE_ACTIVE_SWITCH.indexOf(name) === -1) {
                    MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                        if (exclusive.names.indexOf(name) !== -1 && exclusive.options.allowCollapse === false ) {
                            isSwitch = true;
                        }
                    });
                }
                var icon = this.select(sel);
                if (isSwitch && icon.hasClass('active')) {
                    return;
                } else this.trigger(document, 'menubarToggleDisplay', {name:name});
            };
        });

        this.defaultAttrs(attrs);
        
        this.after('initialize', function() {
            this.$node.html(template({}));
            
            Activity.attachTo( this.select('activityIconSelector') );

            this.on('click', events);

            this.on(document, 'menubarToggleDisplay', function(e, data) {
                var $this = this;
                var icon = this.select(data.name + 'IconSelector');
                var active = icon.hasClass('active');

                if (DISABLE_ACTIVE_SWITCH.indexOf(data.name) === -1) {
                    var isSwitch = false;

                    if (!active) {
                    MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                        if (exclusive.names.indexOf(data.name) !== -1) {
                            isSwitch = true;
                                exclusive.names.forEach(function(name) {
                                    if (name !== data.name) {
                                        var otherIcon = $this.select(name + 'IconSelector');
                                        if ( otherIcon.hasClass('active') ) {
                                            $this.trigger(document, 'menubarToggleDisplay', { name: name, isSwitchButCollapse:true });
                                        }
                                    } else icon.addClass('active');
                                });
                        }
                    });
                    }

                    if ( !isSwitch || data.isSwitchButCollapse ) {
                        icon.toggleClass('active');
                    }

                } else {

                    // Just highlight briefly to show click worked
                    icon.addClass('active');
                    setTimeout(function() {
                        icon.removeClass('active');
                    }, 200);
                }
            });
        });
    }
});
