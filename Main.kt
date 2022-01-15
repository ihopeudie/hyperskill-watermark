package watermark



import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException
import javax.imageio.ImageIO
import kotlin.system.exitProcess

var useAlphaChannel = false
var transparentColor: Color? = null

/**
 * Hyperskill watermark project full
 */

fun main() {
    val mainImage: BufferedImage = readImage("Input the image filename:")
    val watermarkImage: BufferedImage = readImage("Input the watermark image filename:", "watermark")
    if (mainImage.width < watermarkImage.width || mainImage.height < watermarkImage.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(1)
    }
    if (!watermarkImage.colorModel.hasAlpha()) {
        transparentColor = askForTransparencyColor()
    }
    val transparency: Int = readPercentage()
    val isSingleMethod: Boolean = readMethod()

    val pairXY: Pair<Int, Int>? = if (isSingleMethod) readDimensions(mainImage, watermarkImage) else null
    val outputFileName = readOutputFile()
    val watermarkedImage = createWaterMarkedImage(mainImage, watermarkImage, transparency, pairXY)
    ImageIO.write(watermarkedImage, "png", File(outputFileName))
    println("The watermarked image $outputFileName has been created.")
}

fun readDimensions(mainImage: BufferedImage, watermarkImage: BufferedImage): Pair<Int, Int> {
    val diffX = mainImage.width - watermarkImage.width
    val diffY = mainImage.height - watermarkImage.height
    println("Input the watermark position ([x 0-$diffX], [y 0-$diffY]):")
    try {
        val (x, y) = readLine()!!.split(" ").map { Integer.parseInt(it) }
        if (x < 0 || x > diffX || y < 0 || y > diffY) {
            println("The position input is out of range.")
            exitProcess(1)
        }
        return Pair(x, y)
    } catch (e: Exception) {
        println("The position input is invalid.")
        exitProcess(1)
    }

}

fun readMethod(): Boolean {
    println("Choose the position method (single, grid):")
    val pos = readLine()!!
    if (pos != "single" && pos != "grid") {
        println("The position method input is invalid.")
        exitProcess(1)
    }
    return (pos == "single")
}

fun askForTransparencyColor(): Color? {
    println("Do you want to set a transparency color?")
    if (readLine()!! == "yes") {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        try {
            val (r, g, b) = readLine()!!.split(" ").map { Integer.parseInt(it) }
            return Color(r, g, b)
        } catch (e: Exception) {
            println("The transparency color input is invalid.")
            exitProcess(1)
        }
    }
    return null
}

private fun readOutputFile(): String {
    println("Input the output image filename (jpg or png extension):")
    val outputFileName = readLine()!!
    if (!outputFileName.endsWith(".png") && !outputFileName.endsWith(".jpg")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(1)
    }
    return outputFileName
}

fun createWaterMarkedImage(
    mainImage: BufferedImage,
    watermarkImage: BufferedImage,
    transparency: Int,
    pairXy: Pair<Int, Int>?
): BufferedImage {
    val result = BufferedImage(
        mainImage.width, mainImage.height,
        BufferedImage.TYPE_INT_RGB
    )
    if (pairXy != null) {
        //Single watermark with offset in pairXy
        for (x in 0 until mainImage.width) {
            for (y in 0 until mainImage.height) {
                result.setRGB(x, y, mainImage.getRGB(x, y))
            }
        }
        addSingleWaterMark(mainImage, watermarkImage, pairXy, transparency, result)
    } else {
        for (x in 0 until mainImage.width) {
            for (y in 0 until mainImage.height) {
                result.setRGB(x, y, mainImage.getRGB(x, y))
            }
        }
        //building grid
        var x = 0
        var y = 0
        while (canFitToColumn(y, mainImage, watermarkImage)) {
            while (canFitToRow(x, mainImage, watermarkImage)) {
                val pairXyForGrid: Pair<Int, Int> = Pair(x, y)
                addGridWaterMark(mainImage, watermarkImage, pairXyForGrid, transparency, result)
                x += watermarkImage.width
            }
            x = 0
            y += watermarkImage.height
        }
    }
    return result

}

fun addGridWaterMark(
    mainImage: BufferedImage,
    watermarkImage: BufferedImage,
    pairXyForGrid: Pair<Int, Int>,
    transparency: Int,
    result: BufferedImage
) {
    for (y in pairXyForGrid.second until mainImage.height) {
        for (x in pairXyForGrid.first until mainImage.width) {
            val i = Color(mainImage.getRGB(x, y))
            val w = Color(watermarkImage.getRGB(x % watermarkImage.width, y % watermarkImage.height), useAlphaChannel)
            val oc = if (w.alpha == 0 || (w.red == transparentColor?.red && w.green == transparentColor?.green
                        && w.blue == transparentColor?.blue)
            )
                Color(i.red, i.green, i.blue)
            else Color(
                ((100 - transparency) * i.red + transparency * w.red) / 100,
                ((100 - transparency) * i.green + transparency * w.green) / 100,
                ((100 - transparency) * i.blue + transparency * w.blue) / 100
            )
            result.setRGB(x, y, oc.rgb)
        }
    }
}

fun canFitToColumn(y: Int, mi: BufferedImage, wm: BufferedImage): Boolean {
    return (y + wm.height <= mi.height)
}

fun canFitToRow(x: Int, mi: BufferedImage, wm: BufferedImage): Boolean {
    return (x + wm.width <= mi.width)
}

private fun addSingleWaterMark(
    mainImage: BufferedImage,
    watermarkImage: BufferedImage,
    pairXy: Pair<Int, Int>?,
    transparency: Int,
    result: BufferedImage
) {
    for (x in 0 until watermarkImage.width) {
        for (y in 0 until watermarkImage.height) {
            val i = Color(mainImage.getRGB(pairXy?.first?.plus(x) ?: -1, pairXy?.second?.plus(y) ?: -1))
            val w = Color(watermarkImage.getRGB(x, y), useAlphaChannel)
            val isPixelTransparent = (transparentColor != null && transparentColor?.rgb == w.rgb)
            val outputColor =
                if (w.alpha == 0 || isPixelTransparent) Color(i.red, i.green, i.blue)
                else Color(
                    (transparency * w.red + (100 - transparency) * i.red) / 100,
                    (transparency * w.green + (100 - transparency) * i.green) / 100,
                    (transparency * w.blue + (100 - transparency) * i.blue) / 100
                )
            result.setRGB(pairXy?.first?.plus(x) ?: -1,
                pairXy?.second?.plus(y) ?: -1, outputColor.rgb)
        }
    }
}

fun readPercentage(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    val input = readLine()!!
    try {
        val percentage = Integer.parseInt(input, 10)
        if (percentage !in 0..100) {
            println("The transparency percentage is out of range.")
            exitProcess(1)
        }
        return percentage

    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(1)
    }
}

private fun readImage(requestStr: String, imgTitle: String = "image"): BufferedImage {
    println(requestStr)
    val fName = readLine()!!
    val file = readFile(fName)
    val img: BufferedImage = ImageIO.read(file) ?: exitProcess(1)
    if (imgTitle != "image" && img.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        useAlphaChannel = readLine()!! == "yes"
    }
    if (img.colorModel.numComponents < 3) {
        println("The number of $imgTitle color components isn't 3.")
        exitProcess(1)
    }
    if (img.colorModel.pixelSize !in arrayOf(24, 32)) {
        println("The $imgTitle isn't 24 or 32-bit.")
        exitProcess(1)
    }
    return img
}

private fun readFile(fName: String): File {
    val file = File(fName)
    if (!file.exists()) {
        println("The file $fName doesn't exist.")
        exitProcess(1)
    }
    return file
}
