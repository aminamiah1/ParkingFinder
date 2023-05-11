**Mobile Development 2022/23 Portfolio**
# API description

Student ID: `21063494`

The choice of APIs when developing the application was inspired by the app's requirements, as well as the need to balance functionality, simplicity, and effciency.
The APIs used are appropiate for the app's features. 

The decision to use Mapbox API was driven by its excellent performance, easy-to-use interface and wide range of customisation options. The app displays a seamless map view, handles map events and updates the map with nearby parking spots using markers. This API ensures a visually appealing and interactive map that enhances the users experience.

The Android Location API is a crucial component of the app, as it provides the device's current location. By requesting location permission and retreving the last known location, the app can display relevant parking spot suggestions to the user. Additionally, the FusedLocationProviderClient and LocationCallback enable real-time location updates, ensuring the app stays responsive and accurate.

To fetch parking spots near the user's location or a searched address, the Google Places API was chosen. The API has an extensive database of places ensuring comprehensive results. The places API client is intialised, and a request is constructed to find autocomplete predictions for parking places. Detailed place information is then used to create markers on the map.

The OpenStreetMap API is another valuable addition to the app, allowing it to fetch parking spots near the current location. By constructing a URL with latitude and longitude parameters and sending a GET request to the Nominatim API, the app retrieves parking spot data in the form of a JSON array. This API provides an open-source alternative to Google Places API, ensuring a wider range of parking spot options.

I also use the Android Geocoder API which plays a vital role in converting a searched address into a GeoPoint. By retreving a lot of matching addresses and extracting the latitude and longitude from the first result, the app can display parking spots near the searched address.

Lastly, the Android Volley Library is used to handle HTTP requests to the OpenStreetMap API. This library simplifies networking tasks and ensures optimal performance. 

The chosen approach is a balance between effciency and functionality, ensuring that the app is neither over or under engineered. By making appropiate API choices for each feature, the app provides a comprehensive parking spot.
