import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter


def generate_logo(
    output_path="Logo.png",
    size=(128, 128),
    bg_color=(240, 240, 240),
    text_color=(50, 50, 50),
    highlight_color=(255, 255, 255, 150),
    version_color=(100, 100, 100),
    font_name="Sriracha-Regular.ttf",
    main_text="Smooth\nExperience\nBar",
    version_text="V1.0.2"
):
    """Generate application logo with layered text and geometric pattern."""

    # Initialize image and drawing context
    image = Image.new("RGB", size, bg_color)
    draw = ImageDraw.Draw(image)

    # Generate triangle pattern background
    triangle_size = 16
    colors = [
        (173, 216, 230),
        (144, 238, 144),
        (255, 182, 193),
        (255, 255, 153)
    ]

    for y in range(0, size[1] + triangle_size, triangle_size):
        for x in range(0, size[0] + triangle_size, triangle_size):
            color_idx = (x + y) // triangle_size % len(colors)
            points = (
                [(x, y), (x + triangle_size, y), (x, y + triangle_size)]
                if (x + y) // triangle_size % 2 == 0
                else [(x + triangle_size, y), (x + triangle_size, y + triangle_size), (x, y + triangle_size)]
            )
            draw.polygon(points, fill=colors[color_idx])

    # Load fonts with fallback
    script_dir = os.path.dirname(os.path.abspath(__file__))
    font_path = os.path.join(script_dir, "fonts", font_name)

    try:
        main_font = ImageFont.truetype(font_path, 20)
        version_font = ImageFont.truetype(font_path, 12)
    except (OSError, IOError):
        main_font = ImageFont.load_default().font_variant(size=20)
        version_font = ImageFont.load_default().font_variant(size=12)

    # Create text layer with shadow effect
    text_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    text_draw = ImageDraw.Draw(text_layer)
    lines = main_text.split('\n')
    line_height = main_font.getbbox("A")[3]
    total_height = len(lines) * line_height
    y_pos = (size[1] - total_height - 25) / 2

    for line in lines:
        bbox = main_font.getbbox(line)
        text_width = bbox[2] - bbox[0]
        x_pos = (size[0] - text_width) / 2

        # Shadow effect
        text_draw.text((x_pos + 1, y_pos + 1), line, font=main_font, fill=highlight_color)
        text_draw.text((x_pos, y_pos), line, font=main_font, fill=text_color)
        y_pos += line_height

    # Version text
    v_bbox = version_font.getbbox(version_text)
    v_width = v_bbox[2] - v_bbox[0]
    v_x = (size[0] - v_width) / 2

    text_draw.text((v_x + 1, y_pos + 1), version_text, font=version_font, fill=highlight_color)
    text_draw.text((v_x, y_pos), version_text, font=version_font, fill=version_color)

    # Apply blur and composite
    blurred_text = text_layer.filter(ImageFilter.GaussianBlur(0.5))
    image.paste(blurred_text, (0, 0), blurred_text)
    image.save(output_path, quality=95, optimize=True)


if __name__ == "__main__":
    generate_logo()
