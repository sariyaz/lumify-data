var tests = Object.keys(window.__karma__.files).filter(function (file) {
    return (/^\/base\/test\/spec\/.*\.js$/).test(file);
});

//var applyConfiguration = require.config.bind(require);
requirejs(['/base/js/require.config.js'], function(cfg) {

    var requireConfig = $.extend(true, {}, cfg, {

        // Karma serves files from '/base'
        baseUrl: '/base/js',

        paths: {
            chai: '../libs/chai/chai',
            sinon: '../libs/sinon/lib/sinon',
            'sinon-chai': '../libs/sinon-chai/lib/sinon-chai',
            'flight-mocha': '../libs/flight-mocha/lib/flight-mocha'
        },

        shim: {
            sinon: { exports: 'sinon' }
        },

        deps: [ 
            'chai', 
            'sinon', 
            '../libs/es5-shim/es5-shim',
            '../libs/es5-shim/es5-sham'  
        ],

        callback: function(chai, sinon) {
            sinon.spy = sinon.spy || {};

            require([
                    'sinon-chai', 
                    'sinon/util/event',
                    'sinon/call',
                    'sinon/stub',
                    'sinon/spy',
                    'sinon/mock',
                    'flight-mocha'
            ], function(sinonChai) {

                // Use sinon as mocking framework
                chai.use(sinonChai);

                // Expose as global variables
                global.chai = chai;
                global.sinon = sinon;

                // Globals for assertions
                assert = chai.assert;
                expect = chai.expect;

                // Use the twitter flight interface to mocha
                mocha.ui('flight-mocha');
                mocha.options.globals.push( "ejs", "cytoscape", "DEBUG" );

                // Run tests after loading
                if (tests.length) {
                    require(tests, function() {
                        window.__karma__.start();
                    });
                } else window.__karma__.start();
            });

        }

    });
    requireConfig.deps = requireConfig.deps.concat(cfg.deps);
    delete requireConfig.urlArgs;

    window.require = requirejs;
    requirejs.config(requireConfig);
});


