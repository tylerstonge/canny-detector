package edu.oswego.cs.tstonge;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageConverter;

public class Edge_Detector implements PlugInFilter {
    protected ImagePlus image;
    
    // Image properties
    private int width;
    private int height;
    private float[] sobelGx = { -1f, 0f, 1f, -2f, 0f, 2f, -1f, 0f, 1f };
    private float[] sobelGy = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f };
    
    public double sigma;
    public double t1;
    public double t2;
    public boolean applied = false;
    
    @Override
    public int setup(String arg, ImagePlus image) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        this.image = image;
        showDialog();
        
        return DOES_ALL | CONVERT_TO_FLOAT;
    }
    
    @Override
    public void run(ImageProcessor ip) {
        this.width = ip.getWidth();
        this.height = ip.getHeight();
        
        // Convert to greyscale
        ImageConverter ic = new ImageConverter(image);
        ic.convertToGray32();
        
        // Process the image
        process(image.getChannelProcessor());
        
        image.updateAndDraw();
    }
    
    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Canny Edge Detector");
        
        gd.addNumericField("Gaussian Sigma", 2.00, 2);
        gd.addNumericField("T1", 30.00, 2);
        gd.addNumericField("T2", 60.00, 2);
        
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        
        this.sigma = gd.getNextNumber();
        this.t1 = gd.getNextNumber();
        this.t2 = gd.getNextNumber();
        
        return true;
    }
    
    public void process(ImagePlus image) {
        for (int i = 1; i <= image.getStackSize(); i++)
            process(image.getStack().getProcessor(i));
    }
    
    public void process(ImageProcessor ip) {
        if (!applied) {
            // Apply the Gaussian blur
            (new GaussianBlur()).blurGaussian(ip, this.sigma);
            
            ImageProcessor copy = ip.duplicate();
            float[] pixels = (float[]) ip.getPixels();
            float[] directions = new float[pixels.length];
            for (int y = 0; y < height - 1; y++) {
                for (int x = 0; x < width - 1; x++) {
                    // Apply the sobel operator
                    float gx = Float.intBitsToFloat(copy.getPixel(x - 1, y - 1)) * sobelGx[0] +
                             Float.intBitsToFloat(copy.getPixel(x - 1, y)) * sobelGx[3] + 
                             Float.intBitsToFloat(copy.getPixel(x - 1, y + 1)) * sobelGx[6] + 
                             Float.intBitsToFloat(copy.getPixel(x, y - 1)) * sobelGx[1] + 
                             Float.intBitsToFloat(copy.getPixel(x, y)) * sobelGx[4] + 
                             Float.intBitsToFloat(copy.getPixel(x, y + 1)) * sobelGx[7] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y - 1)) * sobelGx[2] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y)) * sobelGx[5] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y + 1)) * sobelGx[8];
                    float gy = Float.intBitsToFloat(copy.getPixel(x - 1, y - 1)) * sobelGy[0] +
                             Float.intBitsToFloat(copy.getPixel(x - 1, y)) * sobelGy[3] + 
                             Float.intBitsToFloat(copy.getPixel(x - 1, y + 1)) * sobelGy[6] + 
                             Float.intBitsToFloat(copy.getPixel(x, y - 1)) * sobelGy[1] + 
                             Float.intBitsToFloat(copy.getPixel(x, y)) * sobelGy[4] + 
                             Float.intBitsToFloat(copy.getPixel(x, y + 1)) * sobelGy[7] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y - 1)) * sobelGy[2] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y)) * sobelGy[5] + 
                             Float.intBitsToFloat(copy.getPixel(x + 1, y + 1)) * sobelGy[8];
                    pixels[x + y * width] = (float) Math.sqrt(Math.pow(gy, 2) + Math.pow(gx, 2));
                    
                    // Find the direction of gradient changes
                    float theta = (float) (Math.atan2(gy, gx)/Math.PI) * 180f;
                    if ((theta >= -22.5 && theta <= 22.5) || (theta >= 157.5 && theta <= -157.5)) {
                        theta = 0f;
                    } else if ((theta > 22.5 && theta <= 67.5) || (theta <= -112.5 && theta > -157.5)) {
                        theta = 45f;
                    } else if ((theta > 67.5 && theta <= 112.5) || (theta <= -67.5 && theta > 112.5)) {
                        theta = 90f;
                    } else if ((theta > 112.5 && theta < 157.5) || (theta < -22.5 && theta > -67.5)) {
                        theta = 135f;
                    }
                    directions[x + y * width] = theta;
                }
            }
            
            // Non-maximum Supression
            float[] filteredMax = new float[pixels.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    switch(Math.round(directions[x + y * width])) {
                        case 0:
                            // Compare East and West
                            if (comparePixels(ip, x, y, -1, 0) && comparePixels(ip, x, y, 1, 0)) {
                                filteredMax[x + y * width] = Float.intBitsToFloat(ip.getPixel(x, y));
                            } else {
                                filteredMax[x + y * width] = 0f;
                            }
                            break;
                        case 90:
                            // Compare north and south
                            if (comparePixels(ip, x, y, 0, -1) && comparePixels(ip, x, y, 0, 1)) {
                                filteredMax[x + y * width] = Float.intBitsToFloat(ip.getPixel(x, y));
                            } else {
                                filteredMax[x + y * width] = 0f;
                            }
                            break;
                        case 135:
                            // Compare NW and SE
                            if (comparePixels(ip, x, y, -1, -1) && comparePixels(ip, x, y, 1, 1)) {
                                filteredMax[x + y * width] = Float.intBitsToFloat(ip.getPixel(x, y));
                            } else {
                                filteredMax[x + y * width] = 0f;
                            }
                            break;
                        case 45:
                            // Compare NE and SW
                            if (comparePixels(ip, x, y, 1, -1) && comparePixels(ip, x, y, -1, 1)) {
                                filteredMax[x + y * width] = Float.intBitsToFloat(ip.getPixel(x, y));
                            } else {
                                filteredMax[x + y * width] = 0f;
                            }
                            break;
                    }
                }
            }
            
            // Follow edges and apply hysteresis
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (pixels[x + y * width] > t2) {
                        filteredMax = followEdge(filteredMax, directions, x, y, directions[x + y * width]);
                    } else {
                        filteredMax[x + y * width] = 0f;
                    }
                }
            }
            
            ip.setPixels(filteredMax);
            applied = true;
        }
    }
    
    public float[] followEdge(float[] pixels, float[] directions, int x, int y, float angle) {
        try {
                if (pixels[x + y * width] > t1) {
                    pixels[x + y * width] = 255f;
                    switch(Math.round(angle)) {
                        case 0:
                            if (directions[(x + 1) + y * width] == angle)
                                followEdge(pixels, directions, x + 1, y, angle);
                            break;
                        case 90:
                            if (directions[x + (y + 1) * width] == angle)
                                followEdge(pixels, directions, x, y + 1, angle);
                            break;
                        case 135:
                            if (directions[(x + 1) + (y + 1) * width] == angle)
                                followEdge(pixels, directions, x + 1, y + 1, angle);
                            break;
                        case 45:
                            if (directions[(x - 1) + (y + 1) * width] == angle)
                                followEdge(pixels, directions, x - 1, y + 1, angle);
                            break;
                    }
                } else {
                    pixels[x + y * width] = 0f;
                }
        } catch (ArrayIndexOutOfBoundsException e) { }
        return pixels;
    }
        
    public void showAbout() {
        IJ.showMessage("Canny Edge Detector", "A Canny Edge Detector implementation");
    }
    
    private boolean comparePixels(ImageProcessor pixels, int x, int y, int offsetX, int offsetY) {
        if (Float.intBitsToFloat(pixels.getPixel(x, y)) > Float.intBitsToFloat(pixels.getPixel(x + offsetX, y + offsetY)))
            return true;
        return false;
    }
}
