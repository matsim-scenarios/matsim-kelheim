<h3 class="found-warnings">Warnings found in 1 module ❌</h3>

#### VspConfigConsistencyCheckerImpl

```
vsp should move away from facilitiesSource=FacilitiesSource.none
You are setting the marginal utility of traveling with mode ride to -12.0. VSP standard is to set this to zero.  Please document carefully why you are using a value different from zero, e.g. by showing distance distributions.
You are setting the marginal utility of traveling with mode bike to -3.0. VSP standard is to set this to zero.  Please document carefully why you are using a value different from zero, e.g. by showing distance distributions.
found qsim.linkDynamics=FIFO; vsp should use PassingQ or talk to kai
found qsim.usePersonIdForMissingVehicleId==true; vsp should set this to false or talk to kai
found `qsim.usingTravelTimeCheckInTeleporation==false'; vsp should try out `true' and report.
you are considering car abailability; vsp config is not doing that.   Instead, we are using a daily monetary constant for car.
travelTimeCalculator is not analyzing different modes separately; vsp default is to do that.  Otherwise, you are using the same travel times for, say, bike and car.
```
<style>
.dash-row.row-warnings .dash-card-frame {
	margin-top: 0;
	margin-bottom: 0;
	padding: 0 0.4em;
}
.dash-row.row-warnings .no-warnings {
	color: #4BB543;
	font-weight: bold;
}
.dash-row.row-warnings .found-warnings {
	color: #ED4337;
	font-weight: bold;
}
.dash-row.row-warnings h4 {
	color: white;
	background: #6f5425;
	font-weight: bold;
	padding: 0.75rem 1.5rem;
	margin-top: 1rem;
	border-radius: 10px 10px 0 0;
}
.dash-row.row-warnings pre {
	background: #f8f3d6;
	color: #6f5425;
	border-radius: 0 0 10px 10px;
	white-space: pre-wrap;
}
</style>