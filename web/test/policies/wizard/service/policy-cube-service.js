describe('policies.wizard.service.policy-cube-service', function () {
  beforeEach(module('webApp'));
  beforeEach(module('served/policy.json'));
  beforeEach(module('served/cube.json'));

  var service, q, rootScope, httpBackend, translate, ModalServiceMock, PolicyModelFactoryMock, CubeModelFactoryMock,
    AccordionStatusServiceMock, UtilsServiceMock, fakeCube2, fakeCube3, resolvedPromiseFunction, rejectedPromiseFunction,
    fakeCube = null;
  var fakePolicy = {};

  beforeEach(module(function ($provide) {
    ModalServiceMock = jasmine.createSpyObj('ModalService', ['openModal']);
    PolicyModelFactoryMock = jasmine.createSpyObj('PolicyModelFactory', ['getCurrentPolicy', 'enableNextStep', 'disableNextStep']);
    CubeModelFactoryMock = jasmine.createSpyObj('CubeFactory', ['getCube', 'isValidCube', 'resetCube', 'getContext', 'setError']);
    AccordionStatusServiceMock = jasmine.createSpyObj('AccordionStatusService', ['resetAccordionStatus']);
    UtilsServiceMock = jasmine.createSpyObj('UtilsService', ['removeItemsFromArray', 'convertDottedPropertiesToJson']);
    UtilsServiceMock.convertDottedPropertiesToJson.and.callFake(function (cube) {
      return cube
    });
    PolicyModelFactoryMock.getCurrentPolicy.and.returnValue(fakePolicy);

    // inject mocks
    $provide.value('ModalService', ModalServiceMock);
    $provide.value('PolicyModelFactory', PolicyModelFactoryMock);
    $provide.value('CubeModelFactory', CubeModelFactoryMock);
    $provide.value('AccordionStatusService', AccordionStatusServiceMock);
    $provide.value('UtilsService', UtilsServiceMock);
  }));

  beforeEach(inject(function (_servedCube_, _servedPolicy_, $q, $rootScope, $httpBackend, $translate) {
    fakeCube = _servedCube_;
    angular.extend(fakePolicy, fakePolicy, _servedPolicy_);

    translate = $translate;
    q = $q;
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    // mocked responses
    ModalServiceMock.openModal.and.callFake(function () {
      var defer = $q.defer();
      defer.resolve();
      return {"result": defer.promise};
    });

    $httpBackend.when('GET', 'languages/en-US.json')
      .respond({});

    CubeModelFactoryMock.getCube.and.returnValue(fakeCube);
    CubeModelFactoryMock.getContext.and.returnValue({"position": 0});

    spyOn(translate, "instant").and.callThrough();

    fakeCube2 = angular.copy(fakeCube);
    fakeCube3 = angular.copy(fakeCube);
    fakeCube2.name = "fakeCube2";
    fakeCube3.name = "fakeCube3";
    fakeCube2.dimensions.push({"field": "fakeCube2 output"});
    fakeCube3.dimensions.push({"field": "fakeCube3 output"});

    resolvedPromiseFunction = function () {
      var defer = q.defer();
      defer.resolve();
      return defer.promise;
    };

    rejectedPromiseFunction = function () {
      var defer = q.defer();
      defer.reject();
      return defer.promise;
    };

  }));

  beforeEach(inject(function (_CubeService_) {
    service = _CubeService_;
  }));


  describe("should be able to show a confirmation modal when cube is going to be removed", function () {
    beforeEach(function () {
      translate.instant.calls.reset();
    });

    afterEach(function () {
      rootScope.$digest();
    });

    it("modal should render the confirm modal template", function () {
      service.showConfirmRemoveCube().then(function () {
        expect(ModalServiceMock.openModal.calls.mostRecent().args[1]).toBe('templates/modal/confirm-modal.tpl.html');
      });
    });

    it("modal should be called to be opened with the correct params", function () {
      var expectedModalResolve = {
        title: function () {
          return "_REMOVE_CUBE_CONFIRM_TITLE_"
        },
        message: ""
      };
      service.showConfirmRemoveCube().then(function () {
        expect(ModalServiceMock.openModal.calls.mostRecent().args[2].title()).toEqual(expectedModalResolve.title());
        expect(ModalServiceMock.openModal.calls.mostRecent().args[2].message()).toEqual(expectedModalResolve.message);
      });
    });
  });


  describe("should be able to find cubes which are using the outputs of the current model", function () {

    it("if the output list is empty or undefined, it returns a json with two empty arrays (cube names and positions)", function () {
      var result = service.findCubesUsingOutputs();
      expect(result.names).toEqual([]);
      expect(result.positions).toEqual([]);

      result = service.findCubesUsingOutputs([]);
      expect(result.names).toEqual([]);
      expect(result.positions).toEqual([]);

      result = service.findCubesUsingOutputs(null);
      expect(result.names).toEqual([]);
      expect(result.positions).toEqual([]);
    });
    describe("if valid output list", function () {

      beforeEach(function () {

      });
      it("it should return a json with the names and positions of the found cubes", function () {
        var outputs = [fakeCube2.dimensions[1].field, fakeCube.dimensions[0].field];
        service.policy.cubes = [fakeCube, fakeCube2, fakeCube3];
        var result = service.findCubesUsingOutputs(outputs);
        //with found cubes
        expect(result.names[0]).toBe("fake cube");
        expect(result.names[1]).toBe("fakeCube2");
        expect(result.positions[0]).toBe(0);
        expect(result.positions[1]).toBe(1);
        //without found cubes
        outputs = ["invented"];
        result = service.findCubesUsingOutputs(outputs);
        expect(result.names.length).toBe(0);
        expect(result.positions.length).toBe(0);
      });
    })
  });

  describe("should be able to validate all cubes created", function () {
    beforeEach(function () {
      service.policy.cubes = [fakeCube, fakeCube2, fakeCube3];
    });
    it("return false if some of the cubes is not valid", function () {
      CubeModelFactoryMock.isValidCube.and.callFake(function (cube) {
        if (cube.name == "fakeCube2") {
          return false;
        } else return true;
      });

      expect(service.areValidCubes()).toBe(false);
    });

    it("return true if all cubes are valid", function () {
      CubeModelFactoryMock.isValidCube.and.returnValue(true);
      expect(service.areValidCubes()).toBe(true);
    });

    it("return false if cube list is empty", function () {
      service.policy.cubes = [];
      CubeModelFactoryMock.isValidCube.and.returnValue(true);
      expect(service.areValidCubes()).toBe(false);
    });
  });


  describe("should be able to add a cube to the policy", function () {

    it("cube is not added if it is not valid", function () {
      CubeModelFactoryMock.isValidCube.and.returnValue(false);
      service.addCube();
      expect(service.policy.cubes.length).toBe(0);
    });

    describe("if cube is valid", function () {
      beforeEach(function () {
        CubeModelFactoryMock.isValidCube.and.returnValue(true);
        service.addCube();
      });

      it("it is added to policy with its order", function () {
        expect(service.policy.cubes.length).toBe(1);
        expect(service.policy.cubes[0].name).toEqual(fakeCube.name);
      });

      it("accordion status is reset with the current length of the cube list", function () {
        expect(AccordionStatusServiceMock.resetAccordionStatus).toHaveBeenCalledWith(service.policy.cubes.length);
      });

      it("next step is enabled", function () {
        expect(PolicyModelFactoryMock.enableNextStep).toHaveBeenCalled();
      });
    });
  });

  describe("should be able to remove the cube of the factory by its id", function () {
    beforeEach(inject(function ($rootScope) {
      service.policy.cubes = [fakeCube, fakeCube2, fakeCube3];
      rootScope = $rootScope;
    }));

    afterEach(function () {
      rootScope.$apply();
    });

    it("cube is removed if confirmation modal is confirmed", function () {
      service.removeCube(0).then(function () { // remove the first cube
        expect(service.policy.cubes.length).toBe(2);
        expect(service.policy.cubes[0]).toBe(fakeCube2);
        expect(service.policy.cubes[1]).toBe(fakeCube3);
      })
    });

    it("cube is not removed if confirmation modal is cancelled", function () {
      ModalServiceMock.openModal.and.callFake(function () {
        var defer = q.defer();
        defer.reject();
        return {"result": defer.promise};
      });
      service.removeCube(0).then(function () { // remove the first cube
      }, function () {
        expect(service.policy.cubes.length).toBe(3);
        expect(service.policy.cubes[0]).toBe(fakeCube);
        expect(service.policy.cubes[1]).toBe(fakeCube2);
        expect(service.policy.cubes[2]).toBe(fakeCube3);
      })
    });

    it("should disable next step if cube list is empty after removing a cube", function () {
      service.policy.cubes = [];
      service.policy.cubes.push(fakeCube);
      service.removeCube().then(function () {
        expect(PolicyModelFactoryMock.disableNextStep).toHaveBeenCalled();
      });
    });
  });

  it("should be able to return if a cube is a new cube by its position", function () {
    service.policy.cubes = [];
    service.policy.cubes.push(fakeCube);
    service.policy.cubes.push(fakeCube);
    service.policy.cubes.push(fakeCube);

    expect(service.isNewCube(0)).toBeFalsy();
    expect(service.isNewCube(2)).toBeFalsy();
    expect(service.isNewCube(3)).toBeTruthy();
  });

  describe("should have a count of created cubes", function () {

    it("it is equal to the cube list of policy when service it is initialized", function () {
      expect(service.getCreatedCubes()).toBe(service.policy.cubes.length);
    });
    it("it is incremented when a cube is added", function () {

      CubeModelFactoryMock.isValidCube.and.returnValue(true);
      var expected = service.getCreatedCubes() + 1;
      service.addCube();
      expect(service.getCreatedCubes()).toBe(expected);
    })
  });

  describe("should be able to save a modified cube", function () {
    beforeEach(function () {
      service.policy.cubes = [];
    });

    it("is saved if it is valid and error is hidden", function () {
      var form = {};
      CubeModelFactoryMock.isValidCube.and.returnValue(true);
      service.saveCube(form);

      expect(service.policy.cubes.length).toBe(1);
      expect(CubeModelFactoryMock.setError).not.toHaveBeenCalled();
    });

    it("is not saved if it is invalid and error is updated to a generic form error", function () {
      var form = {};
      CubeModelFactoryMock.isValidCube.and.returnValue(false);
      service.saveCube(form);

      expect(service.policy.cubes.length).toBe(0);
      expect(CubeModelFactoryMock.setError).toHaveBeenCalled();
    });
  });

  it("should be able to reset the created cube number in order to synchronize it with the cubes of the policy", function () {
    service.policy.cubes.length = 5;
    service.resetCreatedCubes();

    expect(service.getCreatedCubes()).toBe(service.policy.cubes.length);
  })

});
