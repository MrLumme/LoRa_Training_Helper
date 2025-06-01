package lumCode.processingGameBase.lora;

import javafx.util.Pair;
import lumCode.interactables.entities.Button;
import lumCode.processingGameBase.Colors;
import lumCode.processingGameBase.Main;
import lumCode.processingGameBase.Settings;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CaptionEntry {
	private final PApplet p;
	private final Pair<File, File> files;
	private final String name;

	public final PImage img;
	public Pair<Integer, Integer> imgSize;
	public final List<Button> keywords = new CopyOnWriteArrayList<>();
	private boolean changed = false;

	public CaptionEntry(File imageFile, File keywordsFile, PApplet p) throws IOException {
		this.p = p;

		files = new Pair<>(imageFile, keywordsFile);
		name = imageFile.getName();
		img = p.requestImage(imageFile.getAbsolutePath());
		String kws = "";
		for (String line : Files.readAllLines(Paths.get(keywordsFile.getAbsolutePath()))) {
			kws += line;
		}
		p.textSize(Settings.FONT_SIZE);
		p.textFont(Main.font, Settings.FONT_SIZE);
		for (String keyword : kws.split(",")) {
			if (!keyword.trim().equals("")) {
				Button key = new Button(0, 0, (int) ((p.textWidth(keyword) * 1.3)), 20, p, keyword, Main.font) {
					@Override public void action() {
						keywords.remove(this);
						resetKeywordPositions();
					}
				};
				key.setOver(Colors.D_GREY);
				keywords.add(key);
			}
		}
	}

	public void draw() {
		if (imgSize == null) {
			resetImagePosition();
		} else {
			p.image(img, 0, 0, imgSize.getKey(), imgSize.getValue());
			p.textAlign(PConstants.LEFT, PConstants.TOP);
			p.text(name, 10, 10);
			for (Button keyword : keywords) {
				keyword.draw();
			}
		}
	}

	public void addKeyword(String keyword) {
		p.textSize(Settings.FONT_SIZE);
		keyword = keyword.trim().replace(' ', '_').toLowerCase();
		for (Button button : keywords) {
			if (keyword.equals(button.getText())) {
				return;
			}
		}
		keywords.add(new Button(0, 0, (int) ((p.textWidth(keyword) * 1.3)), 20, p, keyword, Main.font) {
			@Override public void action() {
				keywords.remove(this);
			}
		});
		if (imgSize == null) {
			resetImagePosition();
		} else {
			resetKeywordPositions();
		}
		changed = true;
	}

	private void resetImagePosition() {
		if (imageLoaded()) {
			int iw = img.width;
			int ih = img.height;

			double ratio = (double) iw / (double) ih;

			if (ih > Main.SCREEN_HEIGHT) {
				ih = Main.SCREEN_HEIGHT;
				iw = (int) (ih * ratio);
			}
			if (iw > Main.SCREEN_WIDTH / 3 * 2) {
				iw = Main.SCREEN_WIDTH / 3 * 2;
				ih = (int) (iw / ratio);
			}
			imgSize = new Pair<>(iw, ih);
			resetKeywordPositions();
		}
	}

	private void resetKeywordPositions() {
		int dx = imgSize.getKey() + 20;
		int dy = 20;
		for (Button keyword : keywords) {
			if (dx + keyword.getW() + 20 > Main.SCREEN_WIDTH) {
				dy += keyword.getH() + 10;
				dx = imgSize.getKey() + 20;
			}
			keyword.setX(dx);
			keyword.setY(dy);

			dx += keyword.getW() + 10;
		}
	}

	public boolean imageLoaded() {
		return img.width > 0;
	}

	public boolean isChanged() {
		return changed;
	}

	public void saveKeywords() {
		if (changed) {
			String kw = "";
			for (Button keyword : keywords) {
				kw += keyword.getText().trim().replace(' ', '_') + ", ";
			}
			if (kw.trim().length() > 0) {
				kw = kw.substring(0, kw.length() - 2);
				files.getValue().delete();
				try {
					PrintWriter pw = new PrintWriter(files.getValue());
					pw.print(kw);
					pw.flush();
					pw.close();
					changed = false;
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
