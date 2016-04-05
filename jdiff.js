angular.module('Index', [])
  .controller('JDiff', ['$scope', function ($scope) {
    $scope['j-diff-reports'] = [
        ].map(function (jDiffReport) {
          return [jDiffReport.replace(/.*-to-/, ''), jDiffReport];
        });
  }]);
