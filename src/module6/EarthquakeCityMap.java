package module6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Divey Anand
 * */
public class EarthquakeCityMap extends PApplet {
	
	// We will use member variables, instead of local variables, to store the data
	// that the setUp and draw methods will need to access (as well as other methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of earthquakes
	// per country.
	
	// You can ignore this.  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;
	
	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";
	
	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// A counter for quakes that occurred in the ocean
	private int oceanQuakeCount = 0;
	
	// A counter for quakes that occurred within the threat circle of a city
	private int nearbyQuakeCount = 0;
	
	// Average magnitude of nearby quakes
	private float avgMagnitude;
	
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	public void setup() {		
		// (1) Initializing canvas and map tiles
		size(900, 700, OPENGL);
		if (offline) {
		    map = new UnfoldingMap(this, 200, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
		    earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			//map = new UnfoldingMap(this, 200, 50, 650, 600, new Google.GoogleMapProvider());
			map = new UnfoldingMap(this, 200, 50, 650, 600, new Microsoft.HybridProvider());
	
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
		    //earthquakesURL = "2.5_week.atom";
		}
		
		MapUtils.createDefaultEventDispatcher(this, map);
		
		// FOR TESTING: Set earthquakesURL to be one of the testing files by uncommenting
		// one of the lines below.  This will work whether you are online or offline
		//earthquakesURL = "test1.atom";
		//earthquakesURL = "test2.atom";
		
		// Uncomment this line to take the quiz
		//earthquakesURL = "quiz2.atom";
		
		
		// (2) Reading in earthquake data and geometric properties
	    //     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		
		for (Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
		//     STEP 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		    oceanQuakeCount++;
		  }
	    }

	    // could be used for debugging
	    printQuakes();
	 		
	    // (3) Add markers to map
	    //     NOTE: Country markers are not added to the map.  They are used
	    //           for their geometric properties
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    
	    sortAndPrint(5);
	}  // End setup
	
	
	public void draw() {
		background(0);
		map.draw();
		addKey();
		
		if (lastClicked instanceof CityMarker)
			popMenu();
	}
	
	
	// TODO: Add the method:
	private void sortAndPrint(int numToPrint) {
		List<EarthquakeMarker> quakes = new ArrayList<EarthquakeMarker>();
		
		for (Marker m : quakeMarkers) {
			quakes.add((EarthquakeMarker)m);
		}
		
		Collections.sort(quakes);
		
		Object[] quakesArray = quakes.toArray();
				
		if (numToPrint > quakesArray.length) {
			for (int i = 0; i < quakesArray.length; i++) {
				System.out.println((EarthquakeMarker)quakesArray[i]);
			}
		} else {
			for (int i = 0; i < numToPrint; i++) {
				System.out.println((EarthquakeMarker)quakesArray[i]);
			}
		}
	}
	
	// and then call that method from setUp
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		//loop();
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m: markers) {
			if (m.isInside(map, mouseX, mouseY) && lastSelected == null) {
				lastSelected = (CommonMarker)m;
				lastSelected.setSelected(true);
				break;
			}
		}

	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		// TODO: Implement this method
		// Hint: You probably want a helper method or two to keep this code
		// from getting too long/disorganized
		
		if (lastClicked == null) {
			ifMarkerClicked(quakeMarkers);
			ifMarkerClicked(cityMarkers);
			
			if (lastClicked instanceof EarthquakeMarker) {
				hideOtherMarkers(quakeMarkers);
				hideCityMarkers();
			} else if (lastClicked instanceof CityMarker) {
				hideOtherMarkers(cityMarkers);
				hideQuakeMarkers();
			}
		} else {	
			lastClicked.setClicked(false);
			lastClicked = null;
			unhideMarkers();
			
			nearbyQuakeCount = 0;
			avgMagnitude = 0;
		}
	}
	
	//check if a marker has been clicked
	private void ifMarkerClicked(List<Marker> markers) {
		for (Marker m : markers) {
			if (lastClicked != null) {
				break;
			}
			
			if (m.isInside(map, mouseX, mouseY) && !m.isHidden()) {
				lastClicked = (CommonMarker)m;
				lastClicked.setClicked(true);
				break;
			}
		}
	}
	
	//Hides other markers of the given type except the one that has been clicked
	private void hideOtherMarkers(List<Marker> markers) {
		for (Marker m : markers) {
			if (m != lastClicked)
				m.setHidden(true);
		}
	}
	
	//Hides all CityMarkers outside of the quake's threat circle
	private void hideCityMarkers() {
		Location quakeLocation = lastClicked.getLocation();
		
		for (Marker cm : cityMarkers) {
			if ( cm.getDistanceTo(quakeLocation) < ((EarthquakeMarker)lastClicked).threatCircle() ) {
				cm.setHidden(false);
			} else {
				cm.setHidden(true);
			}
		}
	}
	
	//Hides all non-threatening quakes
	private void hideQuakeMarkers() {
		Location cityLocation = lastClicked.getLocation();
		
		for (Marker eq : quakeMarkers) {
			if ( eq.getDistanceTo(cityLocation) < ((EarthquakeMarker)eq).threatCircle() ) {
				eq.setHidden(false);
				
				nearbyQuakeCount++;
				avgMagnitude += ((EarthquakeMarker)eq).getMagnitude();
			} else {
				eq.setHidden(true);
			}
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() {	
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);
		rect(25, 50, 150, 250);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", 50, 75);
		
		fill(color(150, 0, 0));
		triangle(55, 92, 65, 92, 60, 102);
		
		fill(0, 0, 0);
		text("City Marker", 75, 95);
		
		fill(255, 255, 255);
		ellipse(60, 120, 10, 10);
		
		fill(0, 0, 0);
		text("Land Quake", 75, 120);
		
		fill(255, 255, 255);
		rect(55, 140, 10, 10);
		
		fill(0, 0, 0);
		text("Ocean Quake", 75, 145);
		
		text("Size ~ Magnitude", 55, 170);
		
		fill(255, 255, 0);
		ellipse(60, 195, 10, 10);

		fill(0, 0, 0);
		text("Shallow", 75, 195);
		
		fill(0, 0, 255);
		ellipse(60, 215, 10, 10);

		fill(0, 0, 0);
		text("Intermediate", 75, 215);
		
		fill(255, 0, 0);
		ellipse(60, 235, 10, 10);

		fill(0, 0, 0);
		text("Deep", 75, 235);

		fill(255, 255, 255);
		ellipse(60, 255, 10, 10);
		
		fill(0, 0, 0);
		line(55, 250, 65, 260);
		line(65, 250, 55, 260);
		
		text("Past Day", 75, 255);
	}
	
	private void popMenu() {
		fill(255, 255, 255);

		rect(25, 300, 150, 60);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		textSize(12);
				
		CityMarker currCity = (CityMarker)lastClicked;
		String currCityText = (String) currCity.getProperty("name");
		
		text(currCityText, 30, 310);
				
		String nearbyQuakesText = "Nearby Earthquakes" + ": " + Integer.toString(nearbyQuakeCount);
		text(nearbyQuakesText, 30, 330);

		float average = 0f;
		
		if (nearbyQuakeCount != 0)
			average = (float)(avgMagnitude/nearbyQuakeCount);

		String avgMagnitudeText = "Average Magnitude" + ": " + Float.toString(average);				
		text(avgMagnitudeText, 30, 350);
	}
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {
		
		// IMPLEMENT THIS: loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	// You will want to loop through the country markers or country features
	// (either will work) and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property, 
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() 	{
		for (Marker cm : countryMarkers) {
			String name = (String)cm.getProperty("name");
			int landQuakeCount = 0;
			
			for (Marker qm : quakeMarkers) {
				EarthquakeMarker em = (EarthquakeMarker)qm;
				
				if (em.isOnLand) {
					String country = (String)em.getProperty("country");
					
					if (name == country)
						landQuakeCount++;
				}
			}
			
			if (landQuakeCount > 0)
				System.out.println(name + ": " + landQuakeCount);
		}
		
		System.out.println("OCEAN QUAKES: " + oceanQuakeCount + "\n");
	}
	
	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}
}
