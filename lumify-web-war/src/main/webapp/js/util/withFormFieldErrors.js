define(['tpl!./alert'], function(alertTemplate) {
    'use strict';

    return withFormFieldErrors;

    function withFormFieldErrors() {

        this.markFieldErrors = function(error, root) {
            var self = this,
                rootEl = root || this.$node,
                messages = [],
                cls = 'control-group error';

            rootEl.find('.control-group.error')
                .removeClass(cls);

            if (!error) {
                return;
            }

            try {
                if (_.isString(error)) {
                    error = JSON.parse(error);
                }
            } catch(e) { }

            if (_.isObject(error)) {
                _.keys(error).forEach(function(fieldName) {
                    switch(fieldName) {
                        case 'visibilitySource':
                            rootEl.find('.visibility')
                        .addClass(cls);
                        messages.push(error[fieldName]);
                        break;
                    }
                });
            } else {
                messages.push(error || 'Unknown error');
            }

            var errorsContainer = rootEl.find('.errors');
            
            if (errorsContainer.length) {
                errorsContainer.html(
                    alertTemplate({
                        error: messages
                    })
                ).show();
            } else {
                console.warn(
                    'No <div class="errors"/> container found ' +
                    'to display error messages for component "' + 
                    this.describe + '"'
                );
            }

            return messages;
        };
    }
});
