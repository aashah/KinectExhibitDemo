package edu.mines.aashah.modules.SimpleModule;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import processing.core.PConstants;
import processing.core.PImage;
import edu.mines.acmX.exhibit.input_services.hardware.BadFunctionalityRequestException;
import edu.mines.acmX.exhibit.input_services.hardware.HardwareManager;
import edu.mines.acmX.exhibit.input_services.hardware.HardwareManagerManifestException;
import edu.mines.acmX.exhibit.input_services.hardware.UnknownDriverRequest;
import edu.mines.acmX.exhibit.input_services.hardware.devicedata.HandTrackerInterface;
import edu.mines.acmX.exhibit.input_services.hardware.devicedata.RGBImageInterface;
import edu.mines.acmX.exhibit.input_services.hardware.drivers.InvalidConfigurationFileException;
import edu.mines.acmX.exhibit.module_management.modules.ProcessingModule;
import edu.mines.acmX.exhibit.stdlib.graphics.Coordinate3D;
import edu.mines.acmX.exhibit.stdlib.graphics.HandPosition;
import edu.mines.acmX.exhibit.stdlib.input_processing.imaging.RGBImageUtilities;
import edu.mines.acmX.exhibit.stdlib.input_processing.receivers.HandReceiver;

/**
 * A simple module example to demonstrate communicating with the
 * HardwareManager all managing received events within a ProcessingModule
 *   
 * @author Aakash Shah
 * 
 * @see {@link HardwareManager} {@link ProcessingModule}
 */
public class SimpleModule extends ProcessingModule {
	
	public static HardwareManager hm;
	
	private RGBImageInterface imageDriver;
	private HandTrackerInterface handDriver;
	private MyHandReceiver receiver;
	
	/*
	 * A processing module inherits the paradigms of a Processing applet. Here,
	 * we implement the setup method that acts as an entry point for our module.
	 * As such, we initialize a lot of our member variables and register for
	 * events here.
	 */
	public void setup() {
		/*
		 * The hardware manager is a singleton that forms a bridge to
		 * retrieve and communicate with drivers supported (as specified in the
		 * HardwareManager manifest file). 
		 * 
		 * The actual creation of the manager
		 * involves loading and verifying the integrity of the manifest file,
		 * as well as ensuring that a device is connected so that we may
		 * actually obtain information from it.
		 */
		try {
			hm = HardwareManager.getInstance();
		} catch (HardwareManagerManifestException e) {
			System.out.println("Error in the HardwareManager manifest file.");
			e.printStackTrace();
		} 
		
		/*
		 * To actually retrieve the driver, such that we can receive
		 * information, requires requesting it from our instance of the
		 * HardwareManager.
		 * 
		 * In this case, we want to set the size to be the same as the rgbimage
		 * we are going to receive from the driver.
		 */
		try {
			imageDriver = (RGBImageInterface) hm.getInitialDriver("rgbimage");
			size(imageDriver.getRGBImageWidth(), imageDriver.getRGBImageHeight());
			handDriver = (HandTrackerInterface) hm.getInitialDriver("handtracking");
		} catch (BadFunctionalityRequestException e) {
			System.out.println("Functionality unknown (may not be supported)");
			e.printStackTrace();
		} catch (UnknownDriverRequest e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 * This creates a "MyHandReceiver" instance that acts a layer of
		 * abstraction towards dealing with data received by the event manager.
		 */
		receiver = new MyHandReceiver();
		/*
		 * Since hand tracking takes an event-based approach towards delivering
		 * information, we should register our receiver with the EventManager.
		 * In this case, this is done through the driver. However, we can also
		 * do it alternatively with:
		 * 
		 * EventManager.getInstance().registerReceiver(EventType.HAND_CREATED, receiver);
		 */
		handDriver.registerHandCreated(receiver);
		handDriver.registerHandUpdated(receiver);
		handDriver.registerHandDestroyed(receiver);		
		
	}
	
	public void draw() {
		/*
		 * For some hand tracking drivers, it may be necessary to allow it to 
		 * refresh information before re-polling for new data. 
		 */
		handDriver.updateDriver();
		
		/*
		 * The RGBImageInterface provides getVisualData() as a method to
		 * retrieve the raw data from a driver that supports the "rgbimage"
		 * functionality.
		 * 
		 * Here we grab this ByteBuffer, convert it into a BufferedImage, and
		 * then finally into a PImage (Processing's form of an image) so that
		 * we may display it on the screeen.
		 */
		ByteBuffer rawRGBImageData = imageDriver.getVisualData();
		BufferedImage bImg = RGBImageUtilities.byteBufferToImage(
				rawRGBImageData,
				imageDriver.getRGBImageWidth(),
				imageDriver.getRGBImageHeight());
		PImage pImg = buffImageToPImage(bImg);
		image(pImg, 0, 0);
		
		noFill();
		stroke(255, 0, 0);
		/*
		 * To actually display a circle indicated the latest position for a
		 * hand, we must first ask our receiver (which in our example is
		 * keeping track of each hand's latest position) for the hand IDs that
		 * it is tracking.
		 * 
		 * Scroll down to read more about this example's receiver object.
		 */
		for (int id : receiver.getHandIDs()) {
			/*
			 * We grab the latest position and draw a red circle which will
			 * follow the hand. Keep in the mind that we are retrieving a
			 * Coordinate3D, so that should we want to incorporate the depth of
			 * the hand into our module, we may do so.
			 */
			Coordinate3D handPosition = receiver.getHandPosition(id);
			ellipse(handPosition.getX(), handPosition.getY(), 20, 20);
		}
	}
	
	/**
	 * A utility function to convert a BufferedImage into a PImage.
	 * @param bimg BufferedImage to convert
	 * @return a PImage
	 */
	public PImage buffImageToPImage(BufferedImage bimg) {
		PImage img = new PImage(bimg.getWidth(), bimg.getHeight(), PConstants.ARGB);
		bimg.getRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
		return img;
	}
	
	
	/**
	 * This class acts as an extension of the HandReceiver adapter. In this
	 * class, we may override the methods handCreated, handUpdated, and 
	 * handDestroyed to handle the lifecycle of a hand being tracked.
	 * 
	 * In this example, we also include functionality to keep track of a hand's
	 * most recent position.
	 * 
	 * The hand tracking API provides the end-programmer an integer id, and
	 * a Coordinate3D to manage the position of the hand. These two attributes
	 * are wrapped into the class HandPosition which will allow you to grab
	 * this information through getters.
	 * 
	 * @author Aakash Shah
	 */
	class MyHandReceiver extends HandReceiver {
		
		private Map<Integer, Coordinate3D> handPositions;
		
		public MyHandReceiver() {
			handPositions = new HashMap<Integer, Coordinate3D>();
		}
		
		public void handCreated(HandPosition handPos) {
			handPositions.put(handPos.getId(), handPos.getPosition());
		}
		
		public void handUpdated(HandPosition handPos) {
			handPositions.put(handPos.getId(), handPos.getPosition());
		}
		
		public void handDestroyed(int id) {
			handPositions.remove(id);
		}
		
		public Set<Integer> getHandIDs() {
			return handPositions.keySet();
		}
		
		public Coordinate3D getHandPosition(int id) {
			return handPositions.get(id);
		}
	}
}
