package jp.linkserver.glyphvisualizer.glyph

/**
 * Preview-only geometry transcribed from the Nothing Settings resources supplied
 * with the project task. Settings resource names describe UI slots; channel
 * bindings below deliberately keep the existing Barty device-spec semantics.
 */
internal object GlyphOfficialLightPreviewLayouts {
    private const val PHONE2A_BODY_CORNER_RATIO = 22.84f / 120f

    private val phone1Canvas = GlyphPreviewSize(182f, 382f)
    private val phone2Canvas = GlyphPreviewSize(191f, 425f)
    private val phone2aCanvas = GlyphPreviewSize(176f, 374f)
    private val phone3aCanvas = GlyphPreviewSize(176f, 374f)

    private fun point(x: Float, y: Float) = GlyphPreviewPoint(x, y)

    private fun pointDp(x: Float, y: Float, canvas: GlyphPreviewSize) =
        point(x / canvas.width, y / canvas.height)

    private fun rect(left: Float, top: Float, right: Float, bottom: Float) =
        GlyphPreviewRect(left, top, right, bottom)

    private fun rectDp(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        canvas: GlyphPreviewSize
    ) = rect(
        left / canvas.width,
        top / canvas.height,
        (left + width) / canvas.width,
        (top + height) / canvas.height
    )

    private fun cameraDp(x: Float, y: Float, radius: Float, canvas: GlyphPreviewSize) =
        GlyphPreviewCameraMarker(pointDp(x, y, canvas), radius / canvas.width)

    private fun spec(range: GlyphPreviewSpecRange) =
        GlyphPreviewChannelBinding.SpecRange(range)

    private fun unmapped(offset: Int) = GlyphPreviewChannelBinding.Unmapped(offset)

    private fun path(
        channels: GlyphPreviewChannelBinding,
        pathData: String,
        sourceBounds: GlyphPreviewRect,
        bounds: GlyphPreviewRect,
        segmentBounds: GlyphPreviewRect = sourceBounds,
        segmentDirection: GlyphPreviewBarDirection = GlyphPreviewBarDirection.LEFT_TO_RIGHT,
        strokeWidth: Float = 0f,
        arcSegments: GlyphPreviewArcSegments? = null
    ) = GlyphLightPreviewElement.VectorPath(
        channels = channels,
        pathData = pathData,
        sourceBounds = sourceBounds,
        bounds = bounds,
        segmentBounds = segmentBounds,
        segmentDirection = segmentDirection,
        strokeWidth = strokeWidth,
        arcSegments = arcSegments
    )

    private fun phone2Path(
        channels: GlyphPreviewChannelBinding,
        pathData: String,
        segmentBounds: GlyphPreviewRect,
        segmentDirection: GlyphPreviewBarDirection = GlyphPreviewBarDirection.LEFT_TO_RIGHT
    ) = path(
        channels = channels,
        pathData = pathData,
        sourceBounds = rect(0f, 0f, 191f, 425f),
        bounds = rect(0f, 0f, 1f, 1f),
        segmentBounds = segmentBounds,
        segmentDirection = segmentDirection
    )

    private fun phone1Path(
        channels: GlyphPreviewChannelBinding,
        pathData: String,
        segmentBounds: GlyphPreviewRect = rect(0f, 0f, 182f, 382f),
        segmentDirection: GlyphPreviewBarDirection = GlyphPreviewBarDirection.LEFT_TO_RIGHT,
        arcSegments: GlyphPreviewArcSegments? = null
    ) = path(
        channels = channels,
        pathData = pathData,
        sourceBounds = rect(0f, 0f, 182f, 382f),
        bounds = rect(0f, 0f, 1f, 1f),
        segmentBounds = segmentBounds,
        segmentDirection = segmentDirection,
        arcSegments = arcSegments
    )

    private val layouts = mapOf(
        GlyphDeviceProfile.PHONE1 to GlyphLightPreviewLayout(
            canvasSize = phone1Canvas,
            bodyCornerRadius = 0.145f,
            geometryUsesFullCanvas = true,
            contentScale = 1f,
            frameAspectRatio = 0.48f,
            cameraMarkers = listOf(
                cameraDp(31.54f, 32.18f, 8.5f, phone1Canvas),
                cameraDp(31.54f, 68.08f, 8.5f, phone1Canvas)
            ),
            elements = listOf(
                phone1Path(
                    channels = unmapped(0),
                    pathData = "M9.35,68.08V32.18C9.35,20.18 18.89,10.36 30.88,10.01C42.88,9.66 52.96,18.91 53.67,30.88C53.69,31.22 53.57,31.55 53.34,31.8C53.11,32.04 52.79,32.18 52.46,32.18H48.79C48.15,32.18 47.61,31.68 47.57,31.04C46.98,22.69 40.03,16.11 31.54,16.11C22.66,16.11 15.47,23.3 15.47,32.18V68.08C15.47,76.95 22.66,84.14 31.54,84.14C40.41,84.14 47.6,76.95 47.6,68.08V57.7C47.6,57.02 48.15,56.47 48.83,56.47H52.49C53.16,56.47 53.71,57.01 53.71,57.7V68.08C53.71,76 49.48,83.32 42.63,87.28C35.77,91.24 27.31,91.24 20.45,87.28C13.58,83.32 9.35,75.99 9.35,68.08Z"
                ),
                phone1Path(
                    channels = unmapped(1),
                    pathData = "M121.21,62.81C120.51,63.65 120.3,64.79 120.67,65.82C121.04,66.85 121.94,67.59 123.01,67.78C124.07,67.97 125.18,67.58 125.88,66.74L159.59,26.57C160.67,25.28 160.5,23.35 159.21,22.27C157.91,21.18 155.99,21.36 154.91,22.65L121.21,62.81Z"
                ),
                phone1Path(
                    channels = spec(GlyphPreviewSpecRange.C),
                    pathData = "M173.49,194.58C173.49,192.89 172.12,191.53 170.44,191.53C168.75,191.53 167.39,192.9 167.39,194.58V246.4C167.39,248.21 166.77,249.97 165.63,251.38C147.4,273.91 119.98,287.01 91,287.01C62.02,287.01 34.6,273.91 16.37,251.38C15.23,249.97 14.61,248.21 14.61,246.4V135.67C14.61,133.85 15.23,132.1 16.37,130.68C34.46,108.32 61.62,95.25 90.38,95.06C119.14,94.88 146.47,107.6 164.84,129.73C165.54,130.56 166.63,130.97 167.7,130.79C168.78,130.61 169.68,129.86 170.05,128.84C170.43,127.82 170.24,126.67 169.54,125.83C150,102.3 120.93,88.76 90.35,88.96C59.75,89.16 30.87,103.06 11.63,126.85C9.6,129.35 8.5,132.47 8.5,135.68V246.4C8.5,249.61 9.6,252.73 11.63,255.23C31.01,279.2 60.17,293.12 91,293.12C121.83,293.12 150.99,279.2 170.37,255.23C172.4,252.73 173.5,249.61 173.5,246.4V194.58H173.49Z",
                    arcSegments = GlyphPreviewArcSegments(
                        center = point(91f, 191.04f),
                        radiusX = 90f,
                        radiusY = 112f,
                        startAngleDegrees = 2f,
                        sweepAngleDegrees = 325f,
                        channelBoundaryAnglesDegrees = listOf(
                            327f,
                            238f,
                            155f,
                            75f,
                            2f
                        ),
                        endCapPaddingDegrees = 4f
                    )
                ),
                phone1Path(
                    channels = spec(GlyphPreviewSpecRange.D1),
                    pathData = "M90.99,356.73C92.68,356.73 94.04,355.36 94.04,353.68V311.8C94.04,310.12 92.67,308.76 90.99,308.76C89.31,308.76 87.94,310.13 87.94,311.8V353.68C87.94,355.37 89.31,356.73 90.99,356.73Z",
                    segmentBounds = rect(87.94f, 308.76f, 94.04f, 356.73f),
                    segmentDirection = GlyphPreviewBarDirection.BOTTOM_TO_TOP
                ),
                phone1Path(
                    channels = unmapped(2),
                    pathData = "M90.99,372C92.68,372 94.04,370.63 94.04,368.95V367.11C94.04,365.43 92.67,364.07 90.99,364.07C89.31,364.07 87.94,365.43 87.94,367.11V368.95C87.94,370.63 89.31,372 90.99,372Z"
                )
            )
        ),
        GlyphDeviceProfile.PHONE2 to GlyphLightPreviewLayout(
            canvasSize = phone2Canvas,
            bodyCornerRadius = 0.145f,
            geometryUsesFullCanvas = true,
            contentScale = 0.925f,
            frameAspectRatio = 0.48f,
            cameraMarkers = listOf(
                cameraDp(28.2f, 26.7f, 9.7f, phone2Canvas),
                cameraDp(28.2f, 70.7f, 9.7f, phone2Canvas)
            ),
            elements = listOf(
                phone2Path(
                    unmapped(0),
                    "M9.37,55.72V26.65C9.37,16.1 17.73,7.45 28.27,7.1C38.81,6.74 47.73,14.8 48.44,25.32C48.5,26.07 49.11,26.65 49.86,26.65H54.12C54.51,26.65 54.88,26.49 55.15,26.2C55.42,25.92 55.56,25.54 55.53,25.15C54.72,10.75 42.6,-0.39 28.18,0.01C13.76,0.42 2.28,12.22 2.28,26.65V55.72C2.28,56.5 2.92,57.13 3.7,57.13H7.95C8.74,57.13 9.37,56.5 9.37,55.72Z",
                    rect(2f, 0f, 56f, 58f)
                ),
                phone2Path(
                    unmapped(1),
                    "M48.49,50.1C48.49,49.32 49.12,48.68 49.91,48.68H54.16C54.94,48.68 55.58,49.32 55.58,50.1V70.73C55.58,80.75 49.95,89.92 41.02,94.47C32.09,99.02 21.36,98.17 13.26,92.27C12.94,92.04 12.74,91.7 12.69,91.31C12.64,90.92 12.75,90.53 12.99,90.23L15.69,86.93C16.16,86.35 17,86.24 17.61,86.67C23.57,90.91 31.4,91.46 37.9,88.1C44.41,84.75 48.49,78.04 48.49,70.73V50.1Z",
                    rect(12f, 48f, 56f, 99f)
                ),
                phone2Path(
                    unmapped(2),
                    "M169.14,13.06C170.4,11.56 172.63,11.36 174.13,12.62C175.63,13.88 175.83,16.11 174.57,17.61L136.01,63.56C135.41,64.28 134.54,64.73 133.61,64.81C132.67,64.9 131.74,64.6 131.02,64L129.39,62.63C129.1,62.39 128.92,62.04 128.89,61.67C128.86,61.3 128.98,60.92 129.22,60.64L169.14,13.06Z",
                    rect(128f, 11f, 176f, 66f)
                ),
                phone2Path(
                    spec(GlyphPreviewSpecRange.C),
                    "M77.9,97.89C119.45,91.77 161.1,108.26 187.21,141.15C188.43,142.68 188.18,144.91 186.64,146.13C185.11,147.35 182.88,147.09 181.66,145.56C157.13,114.65 117.98,99.15 78.93,104.9C77.68,105.08 76.42,104.58 75.64,103.59C74.85,102.6 74.65,101.26 75.12,100.09C75.59,98.91 76.65,98.07 77.9,97.89Z",
                    rect(74f, 97f, 189f, 148f),
                    GlyphPreviewBarDirection.RIGHT_TO_LEFT
                ),
                phone2Path(
                    unmapped(3),
                    "M46.19,107.25C47.34,106.72 48.68,106.85 49.71,107.59C50.74,108.32 51.3,109.55 51.18,110.81C51.06,112.07 50.28,113.17 49.13,113.7C33.23,120.95 19.27,131.87 8.4,145.56C7.62,146.55 6.36,147.04 5.11,146.86C3.86,146.67 2.8,145.83 2.33,144.66C1.87,143.48 2.07,142.14 2.85,141.15C14.42,126.59 29.27,114.97 46.19,107.25Z",
                    rect(1f, 106f, 52f, 148f)
                ),
                phone2Path(
                    unmapped(4),
                    "M7.09,211.3C7.09,213.25 5.5,214.84 3.54,214.84C1.59,214.84 0,213.25 0,211.3V163.22C0,161.96 0.68,160.79 1.77,160.15C2.87,159.52 4.22,159.52 5.32,160.15C6.41,160.79 7.09,161.96 7.09,163.22V211.3Z",
                    rect(0f, 159f, 8f, 216f)
                ),
                phone2Path(
                    unmapped(5),
                    "M8.4,283.1C32.94,314.01 72.09,329.5 111.14,323.76C112.39,323.57 113.65,324.07 114.43,325.06C115.22,326.06 115.42,327.39 114.95,328.57C114.48,329.74 113.42,330.58 112.17,330.77C70.62,336.88 28.97,320.4 2.86,287.51C2.07,286.51 1.87,285.18 2.34,284C2.8,282.82 3.86,281.98 5.11,281.8C6.36,281.61 7.62,282.11 8.4,283.1Z",
                    rect(1f, 281f, 116f, 332f)
                ),
                phone2Path(
                    unmapped(6),
                    "M187.74,284C188.2,285.18 188,286.51 187.21,287.51C175.65,302.07 160.8,313.69 143.88,321.41C142.73,321.93 141.39,321.8 140.36,321.07C139.33,320.33 138.76,319.11 138.88,317.85C139.01,316.58 139.79,315.48 140.94,314.96C156.84,307.71 170.8,296.79 181.66,283.1C182.45,282.11 183.71,281.61 184.96,281.8C186.21,281.98 187.27,282.82 187.74,284Z",
                    rect(138f, 281f, 189f, 323f)
                ),
                phone2Path(
                    unmapped(7),
                    "M186.53,214.33C184.57,214.33 182.98,215.91 182.98,217.87V254.38C182.98,256.34 184.57,257.93 186.53,257.93C188.48,257.93 190.07,256.34 190.07,254.38V217.87C190.07,215.91 188.48,214.33 186.53,214.33Z",
                    rect(182f, 214f, 191f, 259f)
                ),
                phone2Path(
                    spec(GlyphPreviewSpecRange.D1),
                    "M91.49,357.54V403.68C91.49,405.64 93.08,407.23 95.03,407.23C96.99,407.23 98.58,405.64 98.58,403.68V357.54C98.58,356.27 97.9,355.1 96.81,354.47C95.71,353.83 94.36,353.83 93.26,354.47C92.17,355.1 91.49,356.27 91.49,357.54Z",
                    rect(91f, 353f, 99f, 408f),
                    GlyphPreviewBarDirection.BOTTOM_TO_TOP
                ),
                phone2Path(
                    unmapped(8),
                    "M95.03,413.23C93.08,413.23 91.49,414.82 91.49,416.78V420.52C91.49,422.48 93.08,424.06 95.03,424.06C96.99,424.06 98.58,422.48 98.58,420.52V416.78C98.58,414.82 96.99,413.23 95.03,413.23Z",
                    rect(91f, 413f, 99f, 425f)
                )
            )
        ),
        GlyphDeviceProfile.PHONE2A to GlyphLightPreviewLayout(
            canvasSize = phone2aCanvas,
            bodyCornerRadius = PHONE2A_BODY_CORNER_RATIO,
            geometryUsesFullCanvas = true,
            cameraMarkers = listOf(
                cameraDp(68.405f, 81.67f, 13.875f, phone2aCanvas),
                cameraDp(108.753f, 81.67f, 13.787f, phone2aCanvas)
            ),
            elements = listOf(
                path(
                    channels = spec(GlyphPreviewSpecRange.C),
                    pathData = "M3.552,46.042C7.597,28.167 27.555,8 39.349,3.417",
                    sourceBounds = rect(0f, 0f, 43f, 49f),
                    bounds = rectDp(17.29999f, 14f, 43f, 49f, phone2aCanvas),
                    segmentBounds = rect(0f, 0f, 43f, 49f),
                    strokeWidth = 5.74132f
                ),
                path(
                    channels = spec(GlyphPreviewSpecRange.A),
                    pathData = "M20.067,21.315C21.238,22.489 21.238,24.39 20.067,25.565C18.895,26.739 17,26.739 15.828,25.565C9.824,19.544 4.886,12.537 1.233,4.853C0.517,3.352 1.152,1.557 2.649,0.848C4.146,0.138 5.936,0.766 6.643,2.267C10.003,9.339 14.543,15.776 20.067,21.315Z",
                    sourceBounds = rect(0f, 0f, 21f, 27f),
                    bounds = rectDp(20.93997f, 109.14999f, 21f, 27f, phone2aCanvas)
                ),
                path(
                    channels = spec(GlyphPreviewSpecRange.B),
                    pathData = "M6.179,3.955V53.389C6.179,55.053 4.836,56.399 3.177,56.399C1.517,56.399 0.175,55.053 0.175,53.389V3.955C0.175,2.291 1.517,0.945 3.177,0.945C4.836,0.945 6.179,2.291 6.179,3.955V4.012V3.955Z",
                    sourceBounds = rect(0f, 0f, 7f, 57f),
                    bounds = rectDp(157.12f, 53.5f, 7f, 57f, phone2aCanvas)
                )
            )
        ),
        GlyphDeviceProfile.PHONE3A to GlyphLightPreviewLayout(
            canvasSize = phone3aCanvas,
            bodyCornerRadius = 16.4f / 120f,
            geometryUsesFullCanvas = true,
            cameraMarkers = listOf(
                cameraDp(58.285f, 79.085f, 11.807f, phone3aCanvas),
                cameraDp(87.809f, 79.085f, 11.807f, phone3aCanvas),
                cameraDp(117.333f, 79.085f, 11.807f, phone3aCanvas)
            ),
            elements = listOf(
                path(
                    channels = spec(GlyphPreviewSpecRange.C),
                    pathData = "M5.552,42.968C5.055,44.458 3.445,45.259 1.963,44.763C0.481,44.266 -0.329,42.656 0.168,41.174C5.736,24.502 17.016,10.346 32.013,1.189C33.351,0.372 35.097,0.797 35.915,2.135C36.732,3.473 36.307,5.219 34.969,6.036C21.118,14.488 10.695,27.57 5.552,42.96V42.968Z",
                    sourceBounds = rect(0f, 0f, 37f, 45f),
                    bounds = rectDp(17.01999f, 14.76999f, 37f, 45f, phone3aCanvas),
                    segmentDirection = GlyphPreviewBarDirection.LEFT_TO_RIGHT
                ),
                // Volcano a2 is the five-channel Barty B group, not SDK A.
                path(
                    channels = spec(GlyphPreviewSpecRange.B),
                    pathData = "M5.957,2.028L19.76,20.342C20.705,21.592 20.457,23.37 19.199,24.316C17.942,25.261 16.171,25.013 15.226,23.755L1.422,5.441C0.477,4.191 0.725,2.413 1.983,1.468C3.241,0.522 5.011,0.771 5.957,2.028Z",
                    sourceBounds = rect(0f, 0f, 21f, 25f),
                    bounds = rectDp(20.84998f, 109.88998f, 21f, 25f, phone3aCanvas),
                    segmentDirection = GlyphPreviewBarDirection.RIGHT_TO_LEFT
                ),
                // Volcano b1 is the eleven-channel Barty A group.
                path(
                    channels = spec(GlyphPreviewSpecRange.A),
                    pathData = "M8.649,4.392C8.272,2.87 9.21,1.332 10.732,0.963C12.254,0.595 13.792,1.524 14.161,3.046C18.807,22.025 15.803,42.077 5.805,58.861C5.004,60.207 3.257,60.647 1.912,59.846C0.566,59.045 0.125,57.299 0.926,55.953C10.171,40.443 12.943,21.921 8.649,4.392Z",
                    sourceBounds = rect(0f, 0f, 17f, 61f),
                    bounds = rectDp(146.53f, 57.87997f, 17f, 61f, phone3aCanvas),
                    segmentDirection = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                )
            )
        )
    )

    fun layoutFor(profile: GlyphDeviceProfile): GlyphLightPreviewLayout? = layouts[profile]
}
