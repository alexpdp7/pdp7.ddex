package net.pdp7.ddex.utils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.ddex.xml.avs.avs.ReleaseType;
import net.pdp7.ddex.utils.jaxb.NewReleaseMessage;
import net.pdp7.ddex.utils.jaxb.Release;
import net.pdp7.ddex.utils.jaxb.ReleaseId;
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
		return findReleasesOfType(newReleaseMessage, ReleaseType.TRACK_RELEASE)
			.map(this::getTrackColumns)
			.map((trackColumns) -> { trackColumns.putAll(parentReleaseColumns); return trackColumns; });
	}

	protected Map<String, Object> getParentReleaseColumns(Release parentRelease) {
		Map<String, Object> parentReleaseColumns = new HashMap<String, Object>();
		ReleaseId releaseId = parentRelease.getReleaseId().get(0);
		parentReleaseColumns.put("GRID", releaseId.getGRid());
		parentReleaseColumns.put("EAN", releaseId.getICPN().getValue());
		parentReleaseColumns.put("Catalog Number", releaseId.getCatalogNumber().getValue());
		parentReleaseColumns.put("Release Title", parentRelease.getReferenceTitle().getTitleText().getValue());
		return parentReleaseColumns;
	}

	protected Map<String, Object> getTrackColumns(Release track) {
		Map<String, Object> trackColumns = new HashMap<String, Object>();
		trackColumns.put("ISRC", track.getReleaseId().get(0).getISRC());
		trackColumns.put("Track Title", track.getReferenceTitle().getTitleText().getValue());
		trackColumns.put("Track Subtitle", Optional.ofNullable(track.getReferenceTitle().getSubTitle()).map(SubTitle::getValue).orElse(""));
		return trackColumns;
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
