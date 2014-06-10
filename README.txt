CMPS 121 Final Project - Quadrant GPS

Before using the app:
1. Make sure that Google Play Services is installed and updated. If you are using an emulator such as Genymotion, you can follow the steps on this site to get Google Play Services set up. If Google Play Services are not installed, you cannot view maps of previous trips.
	http://forum.xda-developers.com/showthread.php?t=2528952

2. If you are using it on a physical device, be aware that this app polls the GPS service very quickly(every two seconds), so it could potentially use a lot of data/battery.
3. The minimum API version to run this app is 14, and the target API is 19.

Features Currently SUpported:
1. GPS data is recorded, and stored when the 'Finish' button is pressed. 
2. Viewing a list of trips taken in the past.
3. Viewing a trip taken in the past on a map.
4. Viewing various plots generated from recorded trip data. 

Known Issues:
1. None at this time. Let us know if you can break the app.

How to Use:
1. Start the app
2. Press the record button, and the GPS service starts to record. The home screen will display the total distance traveled, average speed, and total elevation climbed.
3. You can pause or resume the recording at any point.
4. Press finish, and the entire trip is recorded.
5. Press history, and a map will load. Once the map is done loading, it will automatically show the most recent trip on the map.
6. You can use the drop down menu to view a trip from all trips that have been recorded.
7. If you click the details tab, you will see a plot of data from the GPS data collected in that trip
8. In the details tab, there are two dropdown menus: one with a list of all recorded trips, and another to select the type of graph to display for the currently selected trip.
