package net.pdp7.ddex.utils;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.ddex.xml.avs.avs.ArtistRole;
import net.ddex.xml.avs.avs.ParentalWarningType;
import net.ddex.xml.avs.avs.ReleaseType;
import net.pdp7.ddex.utils.jaxb.Artist;
import net.pdp7.ddex.utils.jaxb.DealTerms;
import net.pdp7.ddex.utils.jaxb.NewReleaseMessage;
import net.pdp7.ddex.utils.jaxb.Release;
import net.pdp7.ddex.utils.jaxb.ReleaseDetailsByTerritory;
import net.pdp7.ddex.utils.jaxb.ReleaseId;
import net.pdp7.ddex.utils.jaxb.SoundRecording;
import net.pdp7.ddex.utils.jaxb.SubTitle;

public class DdexToTableConverter {

	public Stream<Map<String, Object>> convert(File file) {
		try {
			return convert((NewReleaseMessage) createUnmarshaller().unmarshal(file));
		} catch (JAXBException e) {
			throw new DdexToTableConverterException.XmlProblem(file, e);
		}
	}

	public Stream<Map<String, Object>> convert(InputStream inputStream) {
		try {
			return convert((NewReleaseMessage) createUnmarshaller().unmarshal(inputStream));
		} catch (JAXBException e) {
			throw new DdexToTableConverterException.XmlProblem(inputStream, e);
		}
	}

	public Stream<Map<String, Object>> convert(NewReleaseMessage newReleaseMessage) {
		Release parentRelease = findParentRelease(newReleaseMessage);
		Map<String, Object> parentReleaseColumns = getParentReleaseColumns(parentRelease);
		AtomicInteger trackCounter = new AtomicInteger();
		return findReleasesOfType(newReleaseMessage, ReleaseType.TRACK_RELEASE)
			.map((track) -> getTrackColumns(track, newReleaseMessage))
			.map((trackColumns) -> { trackColumns.putAll(parentReleaseColumns); return trackColumns; })
			.map((trackColumns) -> { trackColumns.put("Track Number", trackCounter.incrementAndGet()); return trackColumns; });
	}

	protected Map<String, Object> getParentReleaseColumns(Release parentRelease) {
		Map<String, Object> parentReleaseColumns = new HashMap<String, Object>();
		ReleaseId releaseId = parentRelease.getReleaseId().get(0);
		parentReleaseColumns.put("EAN", releaseId.getICPN().getValue());
		parentReleaseColumns.put("UPC", releaseId.getICPN().getValue());
		parentReleaseColumns.put("Release Catalog Number", releaseId.getCatalogNumber() != null ? releaseId.getCatalogNumber().getValue() : "No Catalog Number");
		parentReleaseColumns.put("Release Title", parentRelease.getReferenceTitle().getTitleText().getValue());
		ReleaseDetailsByTerritory firstReleaseDetailsByTerritory = parentRelease.getReleaseDetailsByTerritory().get(0);
		parentReleaseColumns.put("Release Physical Release Date", firstReleaseDetailsByTerritory.getOriginalReleaseDate().getValue());
		parentReleaseColumns.put("Release Digital Release Date", firstReleaseDetailsByTerritory.getOriginalDigitalReleaseDate().getValue());
		return parentReleaseColumns;
	}

	protected Map<String, Object> getTrackColumns(Release track, NewReleaseMessage newReleaseMessage) {
		Map<String, Object> trackColumns = new HashMap<String, Object>();
		String isrc = track.getReleaseId().get(0).getISRC();
		trackColumns.put("ISRC", isrc);
		trackColumns.put("Track Volume", 1);
		trackColumns.put("Track Production Year", getProductionYearFrom(isrc));
		trackColumns.put("Track Title", track.getReferenceTitle().getTitleText().getValue());
		trackColumns.put("Track Subtitle", Optional.ofNullable(track.getReferenceTitle().getSubTitle()).map(SubTitle::getValue).orElse(""));
		trackColumns.put("Duration", Duration.parse(track.getDuration().toString()).getSeconds());

		ReleaseDetailsByTerritory firstDetailsByTerritory = track.getReleaseDetailsByTerritory().get(0);
		trackColumns.put("Track Label", firstDetailsByTerritory.getLabelName().get(0).getValue());
		trackColumns.put("Track Genre", firstDetailsByTerritory.getGenre().stream().map(g -> g.getGenreText().getValue()).collect(Collectors.joining(", ")));
		trackColumns.put("Track Parental Warning", firstDetailsByTerritory.getParentalWarningType().get(0).getValue() == ParentalWarningType.NOT_EXPLICIT ? "No" : "Yes");

		List<Artist> releaseDisplayArtists = firstDetailsByTerritory.getDisplayArtist();
		trackColumns.put("Track Primary Artist", findMainArtist(releaseDisplayArtists).getPartyName().get(0).getFullName().getValue());
		trackColumns.put("Track Featured Artists", findArtistsOfRole(releaseDisplayArtists, ArtistRole.FEATURED_ARTIST)
				.map(a -> a.getPartyName().get(0).getFullName().getValue())
				.collect(Collectors.joining(", ")));
		trackColumns.put("Track Remixers", findArtistsOfUserDefinedRole(releaseDisplayArtists, "Remixer")
				.map(a -> a.getPartyName().get(0).getFullName().getValue())
				.collect(Collectors.joining(", ")));
		trackColumns.put("Track Authors", findArtistsOfRole(releaseDisplayArtists, ArtistRole.AUTHOR)
				.map(a -> a.getPartyName().get(0).getFullName().getValue())
				.collect(Collectors.joining(", ")));
		trackColumns.put("Track Composers", findArtistsOfRole(releaseDisplayArtists, ArtistRole.COMPOSER)
				.map(a -> a.getPartyName().get(0).getFullName().getValue())
				.collect(Collectors.joining(", ")));
		trackColumns.put("Track Copyright Owner", getCopyRightOwnerFromPLineText(track.getPLine().get(0).getPLineText()));
		trackColumns.put("Track Production Owner", track.getPLine().get(0).getPLineText());
		trackColumns.put("Track P Line", track.getPLine().get(0).getPLineText());

		SoundRecording soundRecording = (SoundRecording) track.getReleaseResourceReferenceList().getReleaseResourceReference().get(0).getValue();
		trackColumns.put("Lyrics Language", soundRecording.getLanguageOfPerformance().value());

		DealTerms dealTerms = findDealTerms(track, newReleaseMessage);
		trackColumns.put("Track Territories To Deliver", dealTerms.getTerritoryCode().stream()
				.map((territoryCode) -> territoryCode.getValue())
				.collect(Collectors.joining(",")));
		return trackColumns;
	}

	protected DealTerms findDealTerms(Release track, NewReleaseMessage newReleaseMessage) {
		return newReleaseMessage.getDealList().getReleaseDeal()
				.stream()
				.filter((releaseDeal) -> ((JAXBElement<?>) releaseDeal.getDealReleaseReference().get(0).getValue()).getValue() == track.getReleaseReference().get(0).getValue())
				.reduce((a, b) -> { throw new DdexToTableConverterException.MultipleReleaseDealsFound(newReleaseMessage); })
				.get()
				.getDeal()
				.get(0)
				.getDealTerms();
	}

	protected String getCopyRightOwnerFromPLineText(String pLineText) {
		String[] delimiters = new String[] { "license to ", };
		for(String delimiter : delimiters) {
			if(pLineText.toLowerCase().contains(delimiter)) {
				return pLineText.substring(pLineText.toLowerCase().indexOf(delimiter) + delimiter.length());
			}
		}
		return pLineText;
	}

	protected int getProductionYearFrom(String isrc) {
		int productionYy = Integer.parseInt(isrc.substring(5,7));
		int productionYear = productionYy < 30 ? 2000 + productionYy : 1900 + productionYy;
		return productionYear;
	}

	protected Artist findMainArtist(List<Artist> artists) {
		return findArtistsOfRole(artists, ArtistRole.MAIN_ARTIST)
				.reduce((a, b) -> { throw new DdexToTableConverterException.MultipleMainArtistsFound(artists); })
				.get();
	}
	
	protected Stream<Artist> findArtistsOfRole(List<Artist> artists, ArtistRole role) {
		return artists.stream().filter(a -> a.getArtistRole().get(0).getValue().equals(role));
	}

	protected Stream<Artist> findArtistsOfUserDefinedRole(List<Artist> artists, String userDefinedRole) {
		return artists.stream().filter(a -> a.getArtistRole().get(0).getUserDefinedValue() != null &&
				a.getArtistRole().get(0).getUserDefinedValue().equals(userDefinedRole));
	}

	protected Release findParentRelease(NewReleaseMessage newReleaseMessage) {
		return findReleasesOfType(newReleaseMessage, ReleaseType.ALBUM)
				.reduce((a, b) -> { throw new DdexToTableConverterException.MultipleParentReleasesFound(newReleaseMessage); })
				.get();
	}

	protected Stream<Release> findReleasesOfType(NewReleaseMessage newReleaseMessage, ReleaseType releaseType) {
		return newReleaseMessage
				.getReleaseList()
				.getRelease()
				.stream()
				.filter((Release r) -> r.getReleaseType().get(0).getValue() == releaseType);
	}

	protected Unmarshaller createUnmarshaller() throws JAXBException {
		return JAXBContext.newInstance(NewReleaseMessage.class).createUnmarshaller();
	}

	public static class DdexToTableConverterException extends RuntimeException {
		public DdexToTableConverterException(String reason, Throwable throwable) {
			super(reason, throwable);
		}

		public DdexToTableConverterException(String reason) {
			super(reason);
		}

		public static class MultipleParentReleasesFound extends DdexToTableConverterException {
			public final NewReleaseMessage newReleaseMessage;

			protected MultipleParentReleasesFound(NewReleaseMessage newReleaseMessage) {
				super("Multiple parent releases found in " + newReleaseMessage);
				this.newReleaseMessage = newReleaseMessage;
			}
		}

		public static class MultipleReleaseDealsFound extends DdexToTableConverterException {
			public final NewReleaseMessage newReleaseMessage;

			protected MultipleReleaseDealsFound(NewReleaseMessage newReleaseMessage) {
				super("Multiple release deals found in " + newReleaseMessage);
				this.newReleaseMessage = newReleaseMessage;
			}
		}

		public static class MultipleMainArtistsFound extends DdexToTableConverterException {
			public final List<Artist> artists;

			public MultipleMainArtistsFound(List<Artist> artists) {
				super("Multiple main artists found in " + artists);
				this.artists = artists;
			}
		}

		public static class XmlProblem extends DdexToTableConverterException {
			public XmlProblem(File file, JAXBException e) {
				super("XML problem with " + file, e);
			}

			public XmlProblem(InputStream inputStream, JAXBException e) {
				super("XML problem with " + inputStream, e);
			}
		}
	}
}
