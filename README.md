# Description
I thought an interesting and very visual data set to use was Satellite locations. It seems like there would be an easy to access API for getting the direct Latitude, Longitude, and Altitude. But I could not find any publicly accessible endpoints for this information. NORAD and the Air Force publish information on Satellite locations, but in an obscure format called TLE (Two Line Element) sets. With a Space Perturbation Model, you can derive lots of scientifically interesting data from these TLE sets, but not create Latitude/Longitude/Altitude sets easily. Using the TLE sets published on Celestrack (http://celestrak.com/NORAD/elements/) and an SGP4 propgator based on (http://celestrak.com/publications/AIAA/2006-6753/) I calculated several Lat/Lon/Alt sets based on certain stations for the current hour. To get this information to the Web Globe I decide to create an Amazon Lambda function in Java (https://aws.amazon.com/lambda/) to pull the TLE sets, calculate the orbital positions, and build a json file that is deployed through Amazon S3 to the Web Globe's root directory. This allows me to generate the data sets in a manner that has no compute costs for the client, to update the data automatically at a set interval (No more than once per hour to meet Spack-Track/Celestrack API polling guidlines), and have a highly abstracted/decoupled system.

----

**The WebGL Globe** supports data in `JSON` format, a sample of which you can find [here](https://github.com/dataarts/webgl-globe/blob/master/globe/population909500.json). `webgl-globe` makes heavy use of the [Three.js library](https://github.com/mrdoob/three.js/).

# Data Format

The following illustrates the `JSON` data format that the globe expects:

```javascript
var data = [
    [
    'seriesA', [ latitude, longitude, magnitude, latitude, longitude, magnitude, ... ]
    ],
    [
    'seriesB', [ latitude, longitude, magnitude, latitude, longitude, magnitude, ... ]
    ]
];
```

# Basic Usage

The following code polls a `JSON` file (formatted like the one above) for geo-data and adds it to an animated, interactive WebGL globe.

```javascript
// Where to put the globe?
var container = document.getElementById( 'container' );

// Make the globe
var globe = new DAT.Globe( container );

// We're going to ask a file for the JSON data.
var xhr = new XMLHttpRequest();

// Where do we get the data?
xhr.open( 'GET', 'myjson.json', true );

// What do we do when we have it?
xhr.onreadystatechange = function() {

    // If we've received the data
    if ( xhr.readyState === 4 && xhr.status === 200 ) {

        // Parse the JSON
        var data = JSON.parse( xhr.responseText );

        // Tell the globe about your JSON data
        for ( var i = 0; i < data.length; i ++ ) {
            globe.addData( data[i][1], {format: 'magnitude', name: data[i][0]} );
        }

        // Create the geometry
        globe.createPoints();

        // Begin animation
        globe.animate();

    }

};

// Begin request
xhr.send( null );
```
