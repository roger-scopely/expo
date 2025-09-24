import CoreImage
import CoreImage.CIFilterBuiltins

@objc(OGOStrokeFilter)
public class OGOStrokeFilter: CIFilter {
  @objc public dynamic var inputImage: CIImage?
  public var inputColor: CIColor? = .black
  public var inputThickness: CGFloat = 0
  public var inputThicknessPercent: CGFloat = 0

  override public var outputImage: CIImage? {
    guard
      let src = inputImage,
      let color = inputColor
    else { return inputImage }

    let extent = src.extent
    let thickness = max(inputThickness, inputThicknessPercent * max(extent.width, extent.height))
    guard thickness > 0 else { return src }

    // tint: replace all colour with outline colour, preserving alpha
    let colorFilter = CIFilter.colorMatrix()
    colorFilter.inputImage = src
    colorFilter.rVector = CIVector(x: 0, y: 0, z: 0, w: color.red)
    colorFilter.gVector = CIVector(x: 0, y: 0, z: 0, w: color.green)
    colorFilter.bVector = CIVector(x: 0, y: 0, z: 0, w: color.blue)
    colorFilter.aVector = CIVector(x: 0, y: 0, z: 0, w: 1)
    guard let tinted = colorFilter.outputImage?.clampedToExtent() else { return src }

    // dilate by thickness (px)
    let dilateFilter = CIFilter.morphologyMaximum()
    dilateFilter.inputImage = tinted
    dilateFilter.radius = Float(thickness)
    guard let dilated = dilateFilter.outputImage else { return src }

    // composite over
    let compositeFilter = CIFilter.sourceOverCompositing()
    compositeFilter.inputImage = src
    compositeFilter.backgroundImage = dilated
    return compositeFilter.outputImage?.cropped(to: extent) ?? src
  }
}
