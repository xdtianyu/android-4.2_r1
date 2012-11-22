/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.chimpchat.core;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Base class with basic functionality for ChimpImage implementations.
 */
public abstract class ChimpImageBase implements IChimpImage {
    private static Logger LOG = Logger.getLogger(ChimpImageBase.class.getCanonicalName());

    /**
     * Convert the ChimpImage to a BufferedImage.
     *
     * @return a BufferedImage for this ChimpImage.
     */
    @Override
    public abstract BufferedImage createBufferedImage();

    // Cache the BufferedImage so we don't have to generate it every time.
    private WeakReference<BufferedImage> cachedBufferedImage = null;

    /**
     * Utility method to handle getting the BufferedImage and managing the cache.
     *
     * @return the BufferedImage for this image.
     */
    @Override
    public BufferedImage getBufferedImage() {
        // Check the cache first
        if (cachedBufferedImage != null) {
            BufferedImage img = cachedBufferedImage.get();
            if (img != null) {
                return img;
            }
        }

        // Not in the cache, so create it and cache it.
        BufferedImage img = createBufferedImage();
        cachedBufferedImage = new WeakReference<BufferedImage>(img);
        return img;
    }

    @Override
    public byte[] convertToBytes(String format) {
      BufferedImage argb = convertSnapshot();

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try {
          ImageIO.write(argb, format, os);
      } catch (IOException e) {
          return new byte[0];
      }
      return os.toByteArray();
    }

    @Override
    public boolean writeToFile(String path, String format) {
        if (format != null) {
            return writeToFileHelper(path, format);
        }
        int offset = path.lastIndexOf('.');
        if (offset < 0) {
            return writeToFileHelper(path, "png");
        }
        String ext = path.substring(offset + 1);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(ext);
        if (!writers.hasNext()) {
            return writeToFileHelper(path, "png");
        }
        ImageWriter writer = writers.next();
        BufferedImage image = convertSnapshot();
        try {
            File f = new File(path);
            f.delete();

            ImageOutputStream outputStream = ImageIO.createImageOutputStream(f);
            writer.setOutput(outputStream);

            try {
                writer.write(image);
            } finally {
                writer.dispose();
                outputStream.flush();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public int getPixel(int x, int y) {
        BufferedImage image = getBufferedImage();
        return image.getRGB(x, y);
    }

    private BufferedImage convertSnapshot() {
        BufferedImage image = getBufferedImage();

        // Convert the image to ARGB so ImageIO writes it out nicely
        BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = argb.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return argb;
    }

    private boolean writeToFileHelper(String path, String format) {
        BufferedImage argb = convertSnapshot();

        try {
            ImageIO.write(argb, format, new File(path));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sameAs(IChimpImage other, double percent) {
        BufferedImage otherImage = other.getBufferedImage();
        BufferedImage myImage = getBufferedImage();

        // Easy size check
        if (otherImage.getWidth() != myImage.getWidth()) {
            return false;
        }
        if (otherImage.getHeight() != myImage.getHeight()) {
            return false;
        }

        int[] otherPixel = new int[1];
        int[] myPixel = new int[1];

        int width = myImage.getWidth();
        int height = myImage.getHeight();

        int numDiffPixels = 0;
        // Now, go through pixel-by-pixel and check that the images are the same;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (myImage.getRGB(x, y) != otherImage.getRGB(x, y)) {
                    numDiffPixels++;
                }
            }
        }
        double numberPixels = (height * width);
        double diffPercent = numDiffPixels / numberPixels;
        return percent <= 1.0 - diffPercent;
    }

    // TODO: figure out the location of this class and is superclasses
    private static class BufferedImageChimpImage extends ChimpImageBase {
        private final BufferedImage image;

        public BufferedImageChimpImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        public BufferedImage createBufferedImage() {
            return image;
        }
    }

    public static IChimpImage loadImageFromFile(String path) {
        File f = new File(path);
        if (f.exists() && f.canRead()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new File(path));
                if (bufferedImage == null) {
                    LOG.log(Level.WARNING, "Cannot decode file %s", path);
                    return null;
                }
                return new BufferedImageChimpImage(bufferedImage);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Exception trying to decode image", e);
                return null;
            }
        } else {
            LOG.log(Level.WARNING, "Cannot read file %s", path);
            return null;
        }
    }

    @Override
    public IChimpImage getSubImage(int x, int y, int w, int h) {
        BufferedImage image = getBufferedImage();
        return new BufferedImageChimpImage(image.getSubimage(x, y, w, h));
    }
}
