CMPS 121 Final Project Preview - Quadrant GPS

Before trying to use the app:
1. Make sure that Google Play Services is installed and updated. If you are using an emulator such as Genymotion, you can follow the steps on this site to get Google Play Services set up. If Google Play Services are not installed, you cannot view maps of previous trips.
	http://forum.xda-developers.com/showthread.php?t=2528952

2. If you are using it on a physical device, be aware that this app polls the GPS service very quickly(every two seconds), so it could potentially use a lot of data.
3. The minimum API version to run this app is 14, and the target API is 19.

Features Currently SUpported:
1. GPS data is recorded, and stored when the 'Finish' button is pressed. 
2. Viewing a list of trips taken in the past.
3. Viewing a trip taken in the past on a map.

Features In Progress:
1. Optimizing GPS to get more accurate data.
2. Removing/renaming items from history
3. More information shown on map markers(elev, speed, etc.)

Known Issues:
1. Clicking the back button in the titlebar on the history page will reload the main page, and not all values are properly updated
2. Zoom level on the map when viewing a previous trip does not always zoom to a reasonable amount.
3. The first point (starting location) is not recorded.

How to Use:
1. Start the app
2. Press the play button, and the GPS service starts to record. The home screen will display the total distance traveled, average speed, and total elevation climbed.
3. You can pause or resume the recording at any point.
4. Press finish, and the entire trip is recorded.
5. Press history, and a list of trips in the past is displayed.
6. When a trip from the list is selected, a map will open up, showing all of the points where GPS data was collected and the lines connecting them. If you click on a map marker, it will give you the number of that position(0 being the first point recorded, up until the nth point).