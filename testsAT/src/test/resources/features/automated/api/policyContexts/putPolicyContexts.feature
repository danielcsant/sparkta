@rest
Feature: Test all PUT operations for policyContexts in Sparta Swagger API

	Background: Setup Sparta REST client
		Given I send requests to '${SPARTA_HOST}:${SPARTA_API_PORT}'

	Scenario: Update a policyContext using empty parameter
		Given I send a 'PUT' request to '/policyContext' as 'json'
		Then the service response status must be '400' and its response must contain the text 'Request entity expected but not supplied'
	
	Scenario: Update a policyContext with non-existing status
		When I send a 'PUT' request to '/policyContext' based on 'schemas/policyContexts/status.conf' as 'json' with:
		| id | UPDATE | nonexistingid |
		| status | UPDATE | INVALID |
		Then the service response status must be '400' and its response must contain the text 'No usable value for status'
	
	Scenario: Update a policyContext with non-existing id
		When I send a 'PUT' request to '/policyContext' based on 'schemas/policyContexts/status.conf' as 'json' with:
		| id | UPDATE | nonexistingid |
		| status | UPDATE | Stopped |
		Then the service response status must be '404' and its response must contain the text 'No policy with id nonexistingid'.
	
	Scenario: Update a existing policyContext
		# Add policy context
		When I send a 'POST' request to '/policyContext' based on 'schemas/policies/policy.conf' as 'json' with:
		| name | UPDATE | policycontextvalid |
		| fragments | DELETE | N/A |
		| id | DELETE | N/A |
		# Get policy id
		Given I wait '10' seconds
		When I send a 'GET' request to '/policy/findByName/policycontextvalid'
		Then the service response status must be '200'.
		And I save element '$.id' in environment variable 'previousPolicyID'
		# Update policy context
		When I send a 'PUT' request to '/policyContext' based on 'schemas/policyContexts/status.conf' as 'json' with:
		| id | UPDATE | !{previousPolicyID} |
		| status | UPDATE | Stopped |
		Then the service response status must be '201'.
		# Check the status
		When I send a 'GET' request to '/policyContext'
		Then the service response status must be '200' and its response must contain the text '"status":"Stopped"'

	Scenario: Clean up
		When I send a 'DELETE' request to '/policy/!{previousPolicyID}'
		Then the service response status must be '200'.
	   	# Delete fragments
	  	When I send a 'GET' request to '/fragment/input/name/name'
	  	Then the service response status must be '200'.
	  	And I save element '$.id' in environment variable 'previousFragmentID'
	  	When I send a 'DELETE' request to '/fragment/input/!{previousFragmentID}'
	  	Then the service response status must be '200'.
	  	When I send a 'GET' request to '/fragment/output/name/name'
	  	Then the service response status must be '200'.
	  	And I save element '$.id' in environment variable 'previousFragmentID_2'
	  	When I send a 'DELETE' request to '/fragment/output/!{previousFragmentID_2}'
	  	Then the service response status must be '200'.
	  	When I send a 'GET' request to '/fragment/output/name/name2'
	  	Then the service response status must be '200'.
	  	And I save element '$.id' in environment variable 'previousFragmentID_2'
	  	When I send a 'DELETE' request to '/fragment/output/!{previousFragmentID_2}'
	  	Then the service response status must be '200'.
