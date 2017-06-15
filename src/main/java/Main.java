package edu.oswego.cs.tstonge;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Main {
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Edge_Detector.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        // open the Clown sample
        ImagePlus image = IJ.openImage("http://imagej.net/images/lena.jpg");
        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }
}