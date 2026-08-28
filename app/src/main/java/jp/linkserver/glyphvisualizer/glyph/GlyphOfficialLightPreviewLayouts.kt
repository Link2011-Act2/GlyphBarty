package jp.linkserver.glyphvisualizer.glyph

/**
 * Preview-only geometry transcribed from the Nothing Settings resources supplied
 * with the project task. Settings resource names describe UI slots; channel
 * bindings below deliberately keep the existing Barty device-spec semantics.
 */
internal object GlyphOfficialLightPreviewLayouts {
    private val phone1Canvas = GlyphPreviewSize(179f, 375f)
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

    private val layouts = mapOf(
        GlyphDeviceProfile.PHONE1 to GlyphLightPreviewLayout(
            canvasSize = phone1Canvas,
            bodyCornerRadius = 0.075f,
            geometryUsesFullCanvas = true,
            cameraMarkers = listOf(
                cameraDp(31.60f, 30f, 8.5f, phone1Canvas),
                cameraDp(31.60f, 64f, 8.5f, phone1Canvas)
            ),
            elements = listOf(
                path(
                    channels = unmapped(0),
                    pathData = "M0.57,14.83V38.82C0.57,44.12 3.39,49.01 7.97,51.66C12.54,54.31 18.18,54.31 22.75,51.66C27.32,49.01 30.14,44.12 30.14,38.82V31.88C30.14,31.43 29.77,31.07 29.33,31.07H26.89C26.43,31.07 26.07,31.44 26.07,31.88V38.82C26.07,44.76 21.27,49.57 15.36,49.57C9.44,49.57 4.65,44.76 4.65,38.82V14.83C4.65,8.89 9.44,4.08 15.36,4.08C21.02,4.08 25.66,8.48 26.05,14.07C26.08,14.49 26.43,14.83 26.86,14.83H29.3C29.53,14.83 29.74,14.73 29.9,14.57C30.04,14.4 30.12,14.19 30.11,13.96C29.64,5.96 22.92,-0.23 14.92,0.01C6.93,0.24 0.57,6.81 0.57,14.83Z",
                    sourceBounds = rect(0f, 0f, 30.7f, 53f),
                    bounds = rectDp(9.76999f, 8f, 43.66667f, 78.66667f, phone1Canvas)
                ),
                path(
                    channels = unmapped(1),
                    pathData = "M75.14,35.31C74.67,35.86 74.53,36.63 74.78,37.32C75.03,38 75.62,38.5 76.34,38.63C77.05,38.76 77.78,38.49 78.25,37.93L100.72,11.08C101.45,10.21 101.33,8.93 100.47,8.2C99.61,7.48 98.33,7.59 97.61,8.46L75.14,35.31Z",
                    sourceBounds = rect(74f, 7f, 102f, 39f),
                    bounds = rectDp(118.09f, 19.33f, 39f, 45.33333f, phone1Canvas)
                ),
                path(
                    channels = spec(GlyphPreviewSpecRange.C),
                    pathData = "M107.96,121.35C109.08,121.35 109.99,122.26 109.99,123.39H110V158.03C110,160.18 109.26,162.27 107.92,163.94C94.99,179.96 75.55,189.27 55,189.27C34.45,189.27 15.01,179.96 2.08,163.94C0.74,162.27 0,160.18 0,158.03V84.02C0,81.87 0.74,79.79 2.08,78.12C14.91,62.21 34.17,52.92 54.56,52.79C74.95,52.65 94.33,61.7 107.36,77.44C107.82,77.99 107.96,78.76 107.7,79.45C107.45,80.13 106.85,80.63 106.14,80.75C105.42,80.87 104.69,80.6 104.23,80.04C91.98,65.24 73.76,56.74 54.59,56.86C35.41,56.99 17.31,65.73 5.25,80.68C4.49,81.62 4.07,82.8 4.07,84.01V158.03C4.07,159.25 4.49,160.42 5.25,161.37C17.4,176.43 35.68,185.18 55,185.18C74.32,185.18 92.6,176.43 104.75,161.37C105.51,160.42 105.93,159.25 105.93,158.03V123.39C105.93,122.27 106.83,121.35 107.96,121.35Z",
                    sourceBounds = rect(0f, 52.65f, 110f, 189.27f),
                    bounds = rectDp(8.83333f, 85.14f, 161.33333f, 199.66667f, phone1Canvas),
                    arcSegments = GlyphPreviewArcSegments(
                        center = point(55f, 121.03f),
                        radiusX = 60f,
                        radiusY = 75f,
                        startAngleDegrees = 2f,
                        sweepAngleDegrees = 325f
                    )
                ),
                path(
                    channels = spec(GlyphPreviewSpecRange.D1),
                    pathData = "M54.99,231.79C56.12,231.79 57.03,230.88 57.03,229.76V201.76C57.03,200.63 56.11,199.72 54.99,199.72C53.87,199.72 52.96,200.64 52.96,201.76V229.76C52.96,230.88 53.87,231.79 54.99,231.79Z",
                    sourceBounds = rect(52.5f, 199.5f, 57.5f, 232f),
                    bounds = rectDp(86.33333f, 301.07f, 6.33333f, 50.66667f, phone1Canvas),
                    segmentDirection = GlyphPreviewBarDirection.BOTTOM_TO_TOP
                ),
                path(
                    channels = unmapped(2),
                    pathData = "M54.99,242C56.12,242 57.03,241.08 57.03,239.96V238.73C57.03,237.6 56.11,236.7 54.99,236.7C53.87,236.7 52.96,237.61 52.96,238.73V239.96C52.96,241.08 53.87,242 54.99,242Z",
                    sourceBounds = rect(52.5f, 236.5f, 57.5f, 242.2f),
                    bounds = rectDp(86.33333f, 358.41397f, 6.33333f, 6.33333f, phone1Canvas)
                )
            )
        ),
        GlyphDeviceProfile.PHONE2 to GlyphLightPreviewLayout(
            canvasSize = phone2Canvas,
            bodyCornerRadius = 0.075f,
            geometryUsesFullCanvas = true,
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
            bodyCornerRadius = 22.84f / 120f,
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
