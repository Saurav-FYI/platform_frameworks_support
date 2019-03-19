/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.engine.geometry.Size

/**
 * How a box should be inscribed into another box.
 *
 * See also [applyBoxFit], which applies the sizing semantics of these values
 * (though not the alignment semantics).
 */
enum class BoxFit {
    /**
     * Fill the target box by distorting the source's aspect ratio.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fill.png)
     */
    fill,

    /**
     * As large as possible while still containing the source entirely within the
     * target box.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_contain.png)
     */
    contain,

    /**
     * As small as possible while still covering the entire target box.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_cover.png)
     */
    cover,

    /**
     * Make sure the full width of the source is shown, regardless of
     * whether this means the source overflows the target box vertically.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fitWidth.png)
     */
    fitWidth,

    /**
     * Make sure the full height of the source is shown, regardless of
     * whether this means the source overflows the target box horizontally.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fitHeight.png)
     */
    fitHeight,

    /**
     * Align the source within the target box (by default, centering) and discard
     * any portions of the source that lie outside the box.
     *
     * The source image is not resized.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_none.png)
     */
    none,

    /**
     * Align the source within the target box (by default, centering) and, if
     * necessary, scale the source down to ensure that the source fits within the
     * box.
     *
     * This is the same as `contain` if that would shrink the image, otherwise it
     * is the same as `none`.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_scaleDown.png)
     */
    scaleDown
}

/**
 * Apply a [BoxFit] value.
 *
 * The arguments to this method, in addition to the [BoxFit] value to apply,
 * are two sizes, ostensibly the sizes of an input box and an output box.
 * Specifically, the `inputSize` argument gives the size of the complete source
 * that is being fitted, and the `outputSize` gives the size of the rectangle
 * into which the source is to be drawn.
 *
 * This function then returns two sizes, combined into a single [FittedSizes]
 * object.
 *
 * The [FittedSizes.source] size is the subpart of the `inputSize` that is to
 * be shown. If the entire input source is shown, then this will equal the
 * `inputSize`, but if the input source is to be cropped down, this may be
 * smaller.
 *
 * The [FittedSizes.destination] size is the subpart of the `outputSize` in
 * which to paint the (possibly cropped) source. If the
 * [FittedSizes.destination] size is smaller than the `outputSize` then the
 * source is being letterboxed (or pillarboxed).
 *
 * This method does not express an opinion regarding the alignment of the
 * source and destination sizes within the input and output rectangles.
 * Typically they are centered (this is what [BoxDecoration] does, for
 * instance, and is how [BoxFit] is defined). The [Alignment] class provides a
 * convenience function, [Alignment.inscribe], for resolving the sizes to
 * rects, as shown in the example below.
 *
 * ## Sample code
 *
 * This function paints a [dart:ui.Image] `image` onto the [Rect] `outputRect` on a
 * [Canvas] `canvas`, using a [Paint] `paint`, applying the [BoxFit] algorithm
 * `fit`:
 *
 * ```dart
 * void paintImage(ui.Image image, Rect outputRect, Canvas canvas, Paint paint, BoxFit fit) {
 *   final Size imageSize = new Size(image.width.toDouble(), image.height.toDouble());
 *   final FittedSizes sizes = applyBoxFit(fit, imageSize, outputRect.size);
 *   final Rect inputSubrect = Alignment.center.inscribe(sizes.source, Offset.zero & imageSize);
 *   final Rect outputSubrect = Alignment.center.inscribe(sizes.destination, outputRect);
 *   canvas.drawImageRect(image, inputSubrect, outputSubrect, paint);
 * }
 * ```
 *
 * See also:
 *
 *  * [FittedBox], a widget that applies this algorithm to another widget.
 *  * [paintImage], a function that applies this algorithm to images for painting.
 *  * [DecoratedBox], [BoxDecoration], and [DecorationImage], which together
 *    provide access to [paintImage] at the widgets layer.
 */
fun applyBoxFit(fit: BoxFit, inputSize: Size, outputSize: Size): FittedSizes {
    if (inputSize.height <= 0.0f || inputSize.width <= 0.0f ||
            outputSize.height <= 0.0f || outputSize.width <= 0.0f)
        return FittedSizes(Size.zero, Size.zero)

    val sourceSize: Size
    var destinationSize: Size
    when (fit) {
        BoxFit.fill -> {
            sourceSize = inputSize
            destinationSize = outputSize
        }
        BoxFit.contain -> {
            sourceSize = inputSize
            if (outputSize.width / outputSize.height > sourceSize.width / sourceSize.height)
                destinationSize = Size(
                        sourceSize.width * outputSize.height / sourceSize.height,
                        outputSize.height
                )
            else
                destinationSize = Size(
                        outputSize.width,
                        sourceSize.height * outputSize.width / sourceSize.width
                )
        }
        BoxFit.cover -> {
            if (outputSize.width / outputSize.height > inputSize.width / inputSize.height) {
                sourceSize = Size(
                        inputSize.width,
                        inputSize.width * outputSize.height / outputSize.width
                )
            } else {
                sourceSize = Size(
                        inputSize.height * outputSize.width / outputSize.height,
                        inputSize.height
                )
            }
            destinationSize = outputSize
        }
        BoxFit.fitWidth -> {
            sourceSize = Size(
                    inputSize.width,
                    inputSize.width * outputSize.height / outputSize.width
            )
            destinationSize = Size(
                    outputSize.width,
                    sourceSize.height * outputSize.width / sourceSize.width
            )
        }
        BoxFit.fitHeight -> {
            sourceSize = Size(
                    inputSize.height * outputSize.width / outputSize.height,
                    inputSize.height
            )
            destinationSize = Size(
                    sourceSize.width * outputSize.height / sourceSize.height,
                    outputSize.height
            )
        }
        BoxFit.none -> {
            sourceSize = Size(
                    Math.min(inputSize.width, outputSize.width),
                    Math.min(inputSize.height, outputSize.height))
            destinationSize = sourceSize
        }
        BoxFit.scaleDown -> {
            sourceSize = inputSize
            destinationSize = inputSize
            val aspectRatio = inputSize.width / inputSize.height
            if (destinationSize.width > outputSize.width)
                destinationSize = Size(outputSize.width, outputSize.width / aspectRatio)
            if (destinationSize.height > outputSize.height)
                destinationSize = Size(outputSize.height * aspectRatio, outputSize.height)
        }
    }
    return FittedSizes(sourceSize, destinationSize)
}