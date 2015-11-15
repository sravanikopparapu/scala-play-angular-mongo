var app = angular.module('shipLocator', [
    "ngTable",
    "ngResource",
    'ui.bootstrap'
]);


app.controller("MainController", ["NgTableParams", "$resource", "$http", "$scope", "$uibModal",
    function (NgTableParams, $resource, $http, $scope, $uibModal) {
        var self = this;

        self.tableParams = new NgTableParams({}, {
            getData: function (params) {
                return $resource("/ships").get(params.url()).$promise.then(function (data) {
                    params.total(data.total); // recal. page nav controls
                    return data.results;
                });
            }
        });

        self.refresh = function () {
            self.tableParams.reload().then(function (data) {
                if (data.length === 0 && self.tableParams.total() > 0) {
                    self.tableParams.page(self.tableParams.page() - 1);
                    self.tableParams.reload();
                }
            });
        };

        self.editRow = function (isEditMode, item) {

            var modalInstance = $uibModal.open({
                templateUrl: 'assets/parts/EditModalContent.html',
                controller: 'CreateEditResourceCtrl',
                resolve: {
                    newItem: function () {

                        return isEditMode ?
                        {
                            isEditMode: true,
                            name: item.name,
                            width: item.width,
                            length: item.length,
                            draft: item.draft,
                            lastSeen: item.lastSeen
                        }
                            :
                        {
                            isEditMode: false,
                            name: "",
                            width: 0,
                            length: 0,
                            draft: 0,
                            lastSeen: {lat: 0, lon: 0}
                        }
                    }
                }
            });

            modalInstance.result.then(function (editedItem) {
                if (editedItem.isEditMode) {
                    self.onUpdate(editedItem);
                } else {
                    self.onCreate(editedItem);
                }
            }, function () {
                // on cancel
            });
        };

        self.onCreate = function (item) {
            $scope.resetAlerts();

            $http({
                method: 'POST',
                url: "/ship",
                data: JSON.stringify(item)
            }).success(function (data) {
                self.refresh()
            }).error(function (data, status) {
                if (status > 0) {
                    self.addDangerAlert(status + " : " + data)
                }
                else {
                    self.addDangerAlert("Connection to the api is not available")
                }
            });
        };

        self.onUpdate = function (item) {
            self.resetAlerts();

            $http({
                method: 'PUT',
                url: "/ship",
                data: JSON.stringify(item)
            }).success(function (data) {
                self.refresh()
            }).error(function (data, status) {
                if (status > 0) {
                    self.addDangerAlert(status + " : " + data)
                }
                else {
                    self.addDangerAlert("Connection to the api is not available")
                }
            });
        };

        self.onDelete = function (item) {
            self.resetAlerts();

            var modalInstance = $uibModal.open({
                templateUrl: 'assets/parts/YesNoContent.html',
                controller: 'YesNoModalCtrl',
                resolve: {
                    header: function () {
                        return "Are you sure?"
                    },
                    message: function () {
                        return "Remove ship: " + item.name
                    }
                }
            });

            modalInstance.result.then(function () {
                self.delRow(item)
            }, function () {
                // on cancel
            });
        };

        self.delRow = function (row) {
            $http.delete("/ship/" + row.name);

            self.refresh()
        };

        // alerts

        self.resetAlerts = function () {
            self.alerts = [];
        };

        self.resetAlerts();

        self.addDangerAlert = function (msg) {
            self.alerts.push({type: "danger", msg: msg});
        };

        self.addSuccessAlert = function (msg) {
            self.alerts.push({type: "success", msg: msg});
        };

        self.closeAlert = function (index) {
            self.alerts.splice(index, 1);
        };


    }]);

app.controller('CreateEditResourceCtrl', function ($scope, $uibModalInstance, newItem) {

    $scope.newItem = newItem;
    $scope.isEditMode = newItem.isEditMode;

    $scope.ok = function () {
        $uibModalInstance.close($scope.newItem);
    };

    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    };
});

app.controller('YesNoModalCtrl', function ($scope, $uibModalInstance, header, message) {

    $scope.modalHeader = header;
    $scope.modalMessage = message;

    $scope.ok = function () {
        $uibModalInstance.close();
    };

    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    };
});
