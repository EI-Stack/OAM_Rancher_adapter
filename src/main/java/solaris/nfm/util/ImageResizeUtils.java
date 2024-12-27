package solaris.nfm.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageResizeUtils
{
	public static void resizeImage(final File originalFile, final Integer newImageSize)
	{
		BufferedImage originalImage;
		int newImageHeight = 0;
		int newImageWidth = 0;
		try
		{
			originalImage = ImageIO.read(originalFile);
			float originalImageHeight = originalImage.getHeight();
			float originalImageWidth = originalImage.getWidth();
			if (originalImageWidth > originalImageHeight)
			{
				if (originalImageWidth > newImageSize)
				{
					newImageHeight = Math.round((originalImageHeight / originalImageWidth) * newImageSize);
					newImageWidth = newImageSize;
				} else
				{
					newImageHeight = (int) originalImageHeight;
					newImageWidth = (int) originalImageWidth;
				}
			} else
			{
				if (originalImageHeight > newImageSize)
				{
					newImageHeight = newImageSize;
					newImageWidth = Math.round((originalImageWidth / originalImageHeight) * newImageSize);
				} else
				{
					newImageHeight = (int) originalImageHeight;
					newImageWidth = (int) originalImageWidth;
				}
			}
			// int imageType = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
			// BufferedImage resizedImage = new BufferedImage(newImageWidth, newImageHeight, imageType);
			// Graphics2D g = resizedImage.createGraphics();
			// g.drawImage(originalImage, 0, 0, newImageWidth, newImageHeight, null);
			// g.dispose();
			// ImageIO.write(resizedImage, "png", originalFile);
			BufferedImage resizedImage = new BufferedImage(newImageWidth, newImageHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = resizedImage.createGraphics();
			resizedImage = g2d.getDeviceConfiguration()
					.createCompatibleImage(newImageWidth, newImageHeight,
							Transparency.TRANSLUCENT);
			g2d.dispose();
			g2d = resizedImage.createGraphics();
			@SuppressWarnings("static-access")
			Image from = originalImage.getScaledInstance(newImageWidth, newImageHeight, originalImage.SCALE_AREA_AVERAGING);
			g2d.drawImage(from, 0, 0, null);
			g2d.dispose();
			ImageIO.write(resizedImage, "png", originalFile);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
