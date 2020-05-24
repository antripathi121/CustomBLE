var exec = require('cordova/exec');

module.exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'CustomBLE', 'coolMethod', [arg0]);
};

module.exports.search = function (arg0, success, error) {
    exec(success, error, 'CustomBLE', 'search', [arg0]);
};

module.exports.onClick = function (arg0, success, error) {
    exec(success, error, 'CustomBLE', 'onClick', [arg0]);
}