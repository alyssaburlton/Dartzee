package dartzee.utils

import dartzee.core.bean.paint
import dartzee.core.bean.toBufferedImage
import dartzee.db.PlayerEntity
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import kotlin.math.pow

const val PLAYER_IMAGE_WIDTH = 150
const val PLAYER_IMAGE_HEIGHT = 150

fun splitAvatar(playerOne: PlayerEntity, playerTwo: PlayerEntity, selectedPlayer: PlayerEntity? = null): ImageIcon
{
    val first = playerOne.getAvatarImage()
    val second = playerTwo.getAvatarImage()

    val diagonalOffset = when (selectedPlayer) {
        playerOne -> 1.4
        playerTwo -> 0.6
        else -> 1.0
    }

    val newImage = BufferedImage(PLAYER_IMAGE_WIDTH, PLAYER_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
    newImage.paint { pt ->
        if (pt.x + pt.y == (PLAYER_IMAGE_WIDTH * diagonalOffset).toInt()) {
            Color.BLACK
        } else if (pt.x + pt.y < (PLAYER_IMAGE_WIDTH * diagonalOffset)) {
            val rgb = first.getRGB(pt.x, pt.y)
            val rgbToUse = if (selectedPlayer == playerOne) rgb else greyscale(rgb)
            Color(rgbToUse)
        } else {
            val rgb = second.getRGB(pt.x, pt.y)
            val rgbToUse = if (selectedPlayer == playerTwo) rgb else greyscale(rgb)
            Color(rgbToUse)
        }
    }

    return ImageIcon(newImage)
}
private fun PlayerEntity.getAvatarImage() =
    getAvatar().image.toBufferedImage(PLAYER_IMAGE_WIDTH, PLAYER_IMAGE_HEIGHT)

private fun greyscale(rgb: Int): Int
{
    val r = rgb shr 16 and 0xFF
    val g = rgb shr 8 and 0xFF
    val b = rgb and 0xFF

    // Normalize and gamma correct:
    val rr = (r / 255.0).pow(2.2).toFloat()
    val gg = (g / 255.0).pow(2.2).toFloat()
    val bb = (b / 255.0).pow(2.2).toFloat()

    // Calculate luminance:
    val lum = (0.2126 * rr + 0.7152 * gg + 0.0722 * bb).toFloat()

    // Gamma compand and rescale to byte range:
    val grayLevel = (255.0 * lum.toDouble().pow(1.0 / 2.2)).toInt()
    return (grayLevel shl 16) + (grayLevel shl 8) + grayLevel
}