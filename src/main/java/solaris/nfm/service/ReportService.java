package solaris.nfm.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.system.MailService;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.statistic.Statistic;
import solaris.nfm.model.resource.statistic.StatisticDao;
import solaris.nfm.util.DateTimeUtil;

@Service
@Slf4j
public class ReportService
{
	@Autowired
	@Value("${spring.mail.send-from}")
	private String			sendFrom;
	@Value("${spring.mail.send-to}")
	private String			adminMail;

	@Value("${spring.mail.send-bcc}")
	private String			bccMail;

	@Autowired
	private MailService		mailService;
	@Autowired
	private StatisticDao	dao;
	@Autowired
	private FgcService		service;
	@Autowired
	private LmService		lmService;

	private BaseFont		baseFont;
	private Font			firsetTitleFont;
	private LocalDateTime	timeIntervalStart;
	private LocalDateTime	timeIntervalEnd;
	private Long			msIntervalStart;
	private Long			msIntervalEnd;

	@PostConstruct
	public void init()
	{
		try
		{
			this.baseFont = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
			this.firsetTitleFont = new Font(this.baseFont, 18, Font.NORMAL);// 標題
			// 改由call api日期帶入時間.
			// this.timeIntervalStart = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(0, 0));
			// this.timeIntervalEnd = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 0));
			//
			// this.msIntervalStart = DateTimeUtil.LocalDateTimeToMills(this.timeIntervalStart);
			// this.msIntervalEnd = DateTimeUtil.LocalDateTimeToMills(this.timeIntervalEnd);

		} catch (final DocumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ByteArrayOutputStream createPdf(final LocalDate localDate, final Long msIntervalStart, final Long msIntervalEnd) throws Exception
	{
		// 設置紙張
		final Rectangle rect = new Rectangle(PageSize.A3);
		// 紙張打橫
		rect.rotate();
		// 設置底色
		rect.setBackgroundColor(BaseColor.WHITE);
		// rect.setBackgroundColor(new BaseColor(254, 254, 254));//自己調色
		// 創建文檔實例
		final Document document = new Document(PageSize.A3.rotate());
		// 創建輸出流
		// final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("C://Project/demo01.pdf"));
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
		// 添加中文字體
		final BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
		// 設置字體樣式
		final Font firsetTitleFont = new Font(bf, 18, Font.BOLD);// 標題
		final Font textFont = new Font(bf, 18, Font.NORMAL);// 正常
		final Font redTextFont = new Font(bf, 16, Font.NORMAL, BaseColor.RED); // 正常,紅色
		final Font whiteTextFont = new Font(bf, 16, Font.NORMAL, BaseColor.WHITE); // 正常,紅色
		final Font underlineFont = new Font(bf, 18, Font.UNDERLINE); // 下劃線
		// 設置頁腳
		final PdfReportM1HeaderFooter footer = new PdfReportM1HeaderFooter();
		writer.setPageEvent(footer);
		// ----------------------------------以上最好都寫在document.open()前-----------------------------------------
		// 文件開始
		document.open();

		// 創建文件新頁面（第1頁）
		document.newPage();

		// 段落
		Paragraph pa = new Paragraph();
		pa = new Paragraph("　", firsetTitleFont);
		pa.setSpacingAfter(200);// 設置段落下邊空白距離（段距）
		pa.setAlignment(Element.ALIGN_CENTER);// 段落居中
		document.add(pa);

		pa = new Paragraph();
		// pa = new Paragraph("OAM Report", new Font(bf, 64, Font.BOLD));
		pa = new Paragraph("OAM Report", new Font(FontFamily.TIMES_ROMAN, 64, Font.BOLD));
		pa.setSpacingAfter(100);// 設置段落下邊空白距離（段距）
		pa.setAlignment(Element.ALIGN_CENTER);// 段落居中
		document.add(pa);

		pa = new Paragraph();
		pa = new Paragraph("Data Date : " + localDate.minusDays(1).toString(), new Font(FontFamily.TIMES_ROMAN, 24, Font.NORMAL));
		pa.setAlignment(Element.ALIGN_CENTER);// 段落居中
		document.add(pa);

		final Statistic statistic = this.dao.findTopByDate(localDate.minusDays(1));
		final JsonNode successRate = statistic.getData().path("successRate").path("ue");

		createPageForRegSuccRate(document, successRate, localDate);
		createPageForSvcRegSuccRate(document, successRate, localDate);
		createPageForDeregReqCount(document, successRate, localDate);
		createPageForDeregSuccRate(document, successRate, localDate);
		createPageForHsFgcThroughput(document, statistic, localDate);
		createPageForHsFgcPps(document, statistic, localDate);
		createPageForHsGnbThroughput(document, statistic, localDate);
		createPageForHsGnbPps(document, statistic, localDate);
		createPageForUe(document);
		createChapterForFault(document, "fgc", msIntervalStart, msIntervalEnd);
		createChapterForFault(document, "mec", msIntervalStart, msIntervalEnd);
		createChapterForFault(document, "ric", msIntervalStart, msIntervalEnd);
		createChapterForPerformance(document, "fgc", msIntervalStart, msIntervalEnd);
		createChapterForPerformance(document, "mec", msIntervalStart, msIntervalEnd);
		createChapterForPerformance(document, "ric", msIntervalStart, msIntervalEnd);
		createChapterForGnb(document);
		createChapterForOperationLog(document, msIntervalStart, msIntervalEnd);

		// 文件關閉
		document.close();

		return byteArrayOutputStream;
		// return null;
	}

	/**
	 * 創建文件新頁面 - 註冊成功率
	 */
	private void createPageForRegSuccRate(final Document document, final JsonNode successRate, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "註冊成功率");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		createTitleRow(table, List.of("UE", localDate.minusDays(1).toString()));
		createDataRows(successRate, "regSuccRate", table);
		document.add(table);
	}

	/**
	 * 創建文件新頁面 - 服務請求成功率
	 */
	private void createPageForSvcRegSuccRate(final Document document, final JsonNode successRate, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "服務請求成功率");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		createTitleRow(table, List.of("UE", localDate.minusDays(1).toString()));
		createDataRows(successRate, "svcReqSuccRate", table);
		document.add(table);
	}

	/**
	 * 創建文件新頁面 - 斷線請求次數
	 */
	private void createPageForDeregReqCount(final Document document, final JsonNode successRate, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "斷線請求次數");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		createTitleRow(table, List.of("UE", localDate.minusDays(1).toString()));
		createDataCountRows(successRate, "deregReqCount", table);
		document.add(table);
	}

	/**
	 * 創建文件新頁面 - 斷線成功率
	 */
	private void createPageForDeregSuccRate(final Document document, final JsonNode successRate, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "斷線成功率");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		createTitleRow(table, List.of("UE", localDate.minusDays(1).toString()));
		createDataRows(successRate, "deregSuccRate", table);
		document.add(table);
	}

	private void createPageForHsFgcThroughput(final Document document, final Statistic statistic, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "5GC Historical Traffic Throughput");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		// 產生標題列
		createTitleRow(table, List.of("Title", localDate.minusDays(1).toString()));
		final JsonNode data = statistic.getData().path("dataFlow").path("total").path("throughput");
		createRowsForHistoricalTraffic(table, data);
		document.add(table);

		// 產生圖表.
		// charts(data,document);
	}

	private void createPageForHsFgcPps(final Document document, final Statistic statistic, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "5GC Historical Traffic PPS");
		final PdfPTable table = createTable(2, new int[]{1, 1});
		// 產生標題列
		createTitleRow(table, List.of("Title", localDate.minusDays(1).toString()));
		final JsonNode data = statistic.getData().path("dataFlow").path("total").path("pps");
		createRowsForHistoricalTraffic(table, data);
		document.add(table);

		// 產生圖表.
		// charts(data,document);
	}

	private void createPageForHsGnbThroughput(final Document document, final Statistic statistic, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		final JsonNode gnbGroup = statistic.getData().path("dataFlow").path("gnb");
		final Iterator<String> gnbIpList = gnbGroup.fieldNames();
		if (gnbIpList != null)
		{
			while (gnbIpList.hasNext())
			{
				final String gnbIp = gnbIpList.next();

				createPageTitle(document, "GNB Historical Traffic Throughput (" + gnbIp + ")");
				final PdfPTable table = createTable(2, new int[]{1, 1});
				// 產生標題列
				createTitleRow(table, List.of("Title", localDate.minusDays(1).toString()));

				final JsonNode data = gnbGroup.path(gnbIp).path("throughput");
				createRowsForHistoricalTraffic(table, data);
				document.add(table);

				// 產生圖表.
				// charts(data,document);
			}
		}

	}

	private void createPageForHsGnbPps(final Document document, final Statistic statistic, final LocalDate localDate) throws DocumentException, IOException, ExceptionBase
	{
		final JsonNode gnbGroup = statistic.getData().path("dataFlow").path("gnb");
		final Iterator<String> gnbIpList = gnbGroup.fieldNames();
		if (gnbIpList != null)
		{
			while (gnbIpList.hasNext())
			{
				final String gnbIp = gnbIpList.next();

				createPageTitle(document, "GNB Historical Traffic PPS (" + gnbIp + ")");
				final PdfPTable table = createTable(2, new int[]{1, 1});
				// 產生標題列
				createTitleRow(table, List.of("Title", localDate.minusDays(1).toString()));

				final JsonNode data = gnbGroup.path(gnbIp).path("pps");
				createRowsForHistoricalTraffic(table, data);
				document.add(table);

				// 產生圖表.
				// charts(data,document);
			}
		}

	}

	private void charts(final JsonNode data, final Document document) throws DocumentException, IOException, ExceptionBase
	{
		// 使用JFreeChart設置圖表.
		final CategoryDataset dataset = lineDataset(data);
		final JFreeChart chart = ChartFactory.createLineChart(null, null, "流量", dataset, PlotOrientation.VERTICAL, true, true, true);

		// 圖表背景顏色.
		final CategoryPlot plot = chart.getCategoryPlot();
		plot.setBackgroundPaint(Color.lightGray);// 背景顏色.
		plot.setRangeGridlinePaint(Color.white);// 水平方向背景顏色.

		// 圖表上的線條.
		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
		renderer.setDefaultLinesVisible(true);// 顯示預設線條.
		renderer.setDefaultShapesVisible(true);// 顯示預設圖形可見.
		renderer.setDefaultShapesFilled(false);// 顯示預設圖形點空心.
		renderer.setSeriesLinesVisible(0, true);// 設置第一個線條顯示.
		renderer.setSeriesShapesVisible(0, true);// 設置圖形顯示.
		renderer.setSeriesPaint(0, Color.RED);// 線條顏色.
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));// 線條寬度
		// 圖表上的數字.
		renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("##.##")));// 折線上顯示數字.
		renderer.setDefaultItemLabelsVisible(true);// 顯示預設數字.
		renderer.setDefaultItemLabelFont(new java.awt.Font("SansSerif", Font.NORMAL, 8));
		plot.setRenderer(renderer);

		// X軸 Y軸設定.
		final CategoryAxis lineAxis = plot.getDomainAxis();
		lineAxis.setLabelFont(new java.awt.Font("SansSerif", Font.BOLD, 12));
		lineAxis.setTickLabelFont(new java.awt.Font("SansSerif", Font.BOLD, 12));
		final ValueAxis rangeAxis = plot.getRangeAxis();
		rangeAxis.setLabelFont(new java.awt.Font("SansSerif", Font.BOLD, 12));

		try
		{
			final String fileName = "tempFile";
			final String fileType = ".jpg";
			final File tempFile = File.createTempFile(fileName, fileType);
			// 輸入圖表路徑.，設定圖表長寬.
			final FileOutputStream fos_jpg = new FileOutputStream(tempFile);
			ChartUtils.writeChartAsJPEG(fos_jpg, 1.0f, chart, 600, 500);

			final Image images = Image.getInstance(tempFile.toString());
			images.setAlignment(Image.ALIGN_CENTER);
			document.add(images);

			fos_jpg.close();
			tempFile.deleteOnExit();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}

	}

	private static CategoryDataset lineDataset(final JsonNode data)
	{
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < data.size(); i++)
		{
			dataset.addValue(data.get("max").asInt(), "Maximum", String.valueOf(i + 1));
			dataset.addValue(data.get("min").asInt(), "Minimum", String.valueOf(i + 1));
			dataset.addValue(data.get("mean").asInt(), "Average", String.valueOf(i + 1));
			dataset.addValue(data.get("stddev").asInt(), "Standard Deviation", String.valueOf(i + 1));
		}

		return dataset;
	}

	private static CategoryDataset BarDataset(final JsonNode data)
	{
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		dataset.addValue(data.get("CONNECTED").asInt(), "CONNECTED", String.valueOf(1));
		dataset.addValue(data.get("DISCONNECTED").asInt(), "DISCONNECTED", String.valueOf(1));
		dataset.addValue(data.get("IDLE").asInt(), "IDLE", String.valueOf(1));
		dataset.addValue(data.get("HANDOVER").asInt(), "HANDOVER", String.valueOf(1));

		return dataset;
	}

	private void createRowsForHistoricalTraffic(final PdfPTable table, final JsonNode data)
	{
		table.addCell(createCellForData("最大值"));
		table.addCell(createCellForData(data.path("max").asText()));
		table.addCell(createCellForData("最小值"));
		table.addCell(createCellForData(data.path("min").asText()));
		table.addCell(createCellForData("平均值"));
		table.addCell(createCellForData(data.path("mean").asText()));
		table.addCell(createCellForData("標準差"));
		table.addCell(createCellForData(data.path("stddev").asText()));
	}

	private void createChapterForGnb(final Document document) throws ExceptionBase, DocumentException, IOException
	{
		createPageTitle(document, "GNB List");
		final PdfPTable table = createTable(5, new int[]{1, 1, 1, 1, 1});
		// 產生標題列
		createTitleRow(table, List.of("ID", "Name", "IP", "TA List", "Status"));
		final ArrayNode gnbs = (ArrayNode) this.service.doMethodGet("/gnbs").path("gnblist");
		for (final JsonNode gnb : gnbs)
		{
			table.addCell(createCellForData(gnb.path("id").asText()));
			table.addCell(createCellForData(gnb.path("name").asText()));
			table.addCell(createCellForData(gnb.path("ip").asText()));
			table.addCell(createCellForData(gnb.path("talist").asText()));
			table.addCell(createCellForData(gnb.path("status").asText()));
		}
		document.add(table);
	}

	private void createPageForUe(final Document document) throws DocumentException, IOException, ExceptionBase
	{
		createPageTitle(document, "UE List");
		final PdfPTable table = createTable(5, new int[]{1, 1, 1, 1, 1});
		// 產生標題列
		createTitleRow(table, List.of("IMSI", "UE IP", "連線時間", "斷線時間", "Status"));
		final ArrayNode ues = (ArrayNode) this.service.doMethodGet("/ues?k=").path("uelist");
		for (final JsonNode ue : ues)
		{
			table.addCell(createCellForData(ue.path("imsi").asText()));
			table.addCell(createCellForData(ue.path("ue_ip").asText()));
			table.addCell(createCellForData(DateTimeUtil.castLocalDateTimeToString(DateTimeUtil.millsToLocalDateTime(ue.path("last_connection_time").asLong()))));
			table.addCell(createCellForData(DateTimeUtil.castLocalDateTimeToString(DateTimeUtil.millsToLocalDateTime(ue.path("last_disconnection_time").asLong()))));
			table.addCell(createCellForData(ue.path("status").asText()));
		}
		document.add(table);
	}

	private void createChapterForFault(final Document document, final String networkName, final Long msIntervalStart, final Long msIntervalEnd) throws DocumentException, IOException, ExceptionBase
	{
		final String url = "/history/" + networkName + "/fm?_page=1&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final JsonNode pagination = this.lmService.doMethodGet(url).path("pagination");
		// log.debug("TotalElements={}", pagination.path("totalElements").asInt());
		if (pagination != null)
		{
			for (int pageNo = 0; pageNo < pagination.path("totalPages").asInt(); pageNo++)
			{
				createPageForFault(document, pageNo + 1, networkName, msIntervalStart, msIntervalEnd);
			}
		}

	}

	private void createPageForFault(final Document document, final Integer pageNo, final String networkName, final Long msIntervalStart, final Long msIntervalEnd)
			throws DocumentException, IOException, ExceptionBase
	{
		final String fixedNetworkName = networkName.equals("fgc") ? "5GC" : networkName.toUpperCase();
		final String title = "Fault Alarm (" + fixedNetworkName + ") - Page #" + pageNo;
		createPageTitle(document, title);
		final PdfPTable table = createTable(6, new int[]{1, 1, 1, 1, 1, 1});
		// 產生標題列
		createTitleRow(table, List.of("Severity", "Cleared", "NF", "Alarm", "Message", "Event Time"));

		final String url = "/history/" + networkName + "/fm?_page=" + pageNo + "&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final ArrayNode logs = (ArrayNode) this.lmService.doMethodGet(url).path("content");
		for (final JsonNode log : logs)
		{
			final String networkType = log.path("networkType").asText().equals("fgc") ? "5GC" : log.path("networkType").asText().toUpperCase();
			final String source = networkType.equalsIgnoreCase("ric") ? log.path("fieldId").asText() : log.path("source").asText();
			final String message = log.path("description").asText().equals("") ? log.path("faultErrorCode").asText().split("\\.")[0] : log.path("description").asText();

			table.addCell(createCellForData(log.path("severity").asText()));
			table.addCell(createCellForData(log.path("cleared").asText()));
			table.addCell(createCellForData(networkType));
			table.addCell(createCellForData(source));
			table.addCell(createCellForData(message));
			table.addCell(createCellForData(castIsoTime(log.path("time").asText())));
		}
		document.add(table);
	}

	private void createChapterForPerformance(final Document document, final String networkName, final Long msIntervalStart, final Long msIntervalEnd)
			throws DocumentException, IOException, ExceptionBase
	{
		final String url = "/history/" + networkName + "/pm?_page=1&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final JsonNode pagination = this.lmService.doMethodGet(url).path("pagination");
		// log.debug("TotalElements={}", pagination.path("totalElements").asInt());
		if (pagination != null)
		{
			for (int pageNo = 0; pageNo < pagination.path("totalPages").asInt(); pageNo++)
			{
				createPageForPerformance(document, pageNo + 1, networkName, msIntervalStart, msIntervalEnd);
			}
		}

	}

	private void createPageForPerformance(final Document document, final Integer pageNo, final String networkName, final Long msIntervalStart, final Long msIntervalEnd)
			throws DocumentException, IOException, ExceptionBase
	{
		final String fixedNetworkName = networkName.equals("fgc") ? "5GC" : networkName.toUpperCase();
		final String title = "Performance Alarm (" + fixedNetworkName + ") - Page #" + pageNo;
		createPageTitle(document, title);
		final PdfPTable table = createTable(5, new int[]{1, 1, 1, 1, 1});
		// 產生標題列
		createTitleRow(table, List.of("Severity", "Cleared", "Measurement", "NF", "Event Time"));

		final String url = "/history/" + networkName + "/pm?_page=" + pageNo + "&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final ArrayNode logs = (ArrayNode) this.lmService.doMethodGet(url).path("content");
		for (final JsonNode log : logs)
		{
			final String networkType = log.path("networkType").asText().equals("fgc") ? "5GC" : log.path("networkType").asText().toUpperCase();

			table.addCell(createCellForData(log.path("severity").asText()));
			table.addCell(createCellForData(log.path("cleared").asText()));
			table.addCell(createCellForData(log.path("name").asText()));
			table.addCell(createCellForData(networkType));
			table.addCell(createCellForData(castIsoTime(log.path("time").asText())));
		}
		document.add(table);
	}

	private void createChapterForOperationLog(final Document document, final Long msIntervalStart, final Long msIntervalEnd) throws DocumentException, IOException, ExceptionBase
	{
		final String url = "/operation/systemLog?_page=1&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final JsonNode pagination = this.lmService.doMethodGet(url).path("pagination");
		// log.debug("TotalElements={}", pagination.path("totalElements").asInt());
		if (pagination != null)
		{
			for (int pageNo = 0; pageNo < pagination.path("totalPages").asInt(); pageNo++)
			{
				createPageForOperationLog(document, pageNo + 1, msIntervalStart, msIntervalEnd);
			}
		}

	}

	private void createPageForOperationLog(final Document document, final Integer pageNo, final Long msIntervalStart, final Long msIntervalEnd) throws DocumentException, IOException, ExceptionBase
	{
		final String title = "Operation Log - Page #" + pageNo;
		createPageTitle(document, title);
		final PdfPTable table = createTable(8, new int[]{2, 1, 7, 3, 4, 1, 3, 2});
		// 產生標題列
		createTitleRow(table, List.of("User Name", "Method", "Input", "Operation", "URI", "Response", "Response Body", "Event Time"));

		final String url = "/operation/systemLog?_page=" + pageNo + "&_size=30&_sort=time+desc&_startTime=" + msIntervalStart + "&_endTime=" + msIntervalEnd;
		final ArrayNode logs = (ArrayNode) this.lmService.doMethodGet(url).path("content");
		for (final JsonNode log : logs)
		{
			table.addCell(createCellForData(log.path("userName").asText()));
			table.addCell(createCellForData(log.path("requestMethod").asText()));
			table.addCell(createCellForData(log.path("operationInput").asText()));
			table.addCell(createCellForData(log.path("operationName").asText()));
			table.addCell(createCellForData(log.path("requestUri").asText()));
			table.addCell(createCellForData(log.path("responseStatusCode").asText()));
			table.addCell(createCellForData(log.path("operationOutput").asText()));
			final String time = castIsoTime(log.path("time").asText());
			table.addCell(createCellForData(time));
		}
		document.add(table);
	}

	private void createDataRows(final JsonNode successRate, final String fieldName, final PdfPTable table)
	{
		final DecimalFormat df = new DecimalFormat("0.00%");
		final Iterator<String> successRateUes = successRate.fieldNames();
		if (successRateUes != null)
		{
			while (successRateUes.hasNext())
			{
				final String ue = successRateUes.next();
				final String regSuccRate = df.format(successRate.path(ue).path(fieldName).asDouble());

				table.addCell(createCellForData(ue));
				table.addCell(createCellForData(regSuccRate));
			}
		}

	}

	private void createDataCountRows(final JsonNode successRate, final String fieldName, final PdfPTable table)
	{
		final Iterator<String> successRateUes = successRate.fieldNames();
		if (successRateUes != null)
		{
			while (successRateUes.hasNext())
			{
				final String ue = successRateUes.next();
				final String count = successRate.path(ue).path(fieldName).asText();

				table.addCell(createCellForData(ue));
				table.addCell(createCellForData(count));
			}
		}

	}

	private void createTitleRow(final PdfPTable pt, final List<String> titles) throws DocumentException, IOException
	{
		for (final String title : titles)
		{
			pt.addCell(createCellForTitle(title));
		}
	}

	private PdfPCell createCellForTitle(final String title)
	{
		// 添加中文字體
		final Font textFont = new Font(this.baseFont, 10, Font.NORMAL, BaseColor.WHITE);

		final PdfPCell cell = new PdfPCell();
		cell.setFixedHeight(20f);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setUseAscender(true);
		cell.setBackgroundColor(new BaseColor(41, 127, 186));
		cell.setBorderColor(BaseColor.WHITE);
		cell.setPaddingLeft(5f);
		final Paragraph p = new Paragraph(title, textFont);
		p.setAlignment(Element.ALIGN_LEFT);
		cell.addElement(p);

		return cell;
	}

	private PdfPCell createCellForData(final String data)
	{
		final Font textFont = new Font(this.baseFont, 10, Font.NORMAL);  // 正常

		final PdfPCell cell = new PdfPCell();
		cell.setFixedHeight(20f);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setUseAscender(true);
		cell.disableBorderSide(Rectangle.NO_BORDER);
		cell.setBorderColor(BaseColor.WHITE);
		cell.setPaddingLeft(5f);
		final Paragraph p = new Paragraph(data, textFont);
		p.setAlignment(Element.ALIGN_LEFT);
		cell.addElement(p);

		return cell;
	}

	private void createPageTitle(final Document document, final String title) throws DocumentException, IOException, ExceptionBase
	{
		// 創建文件新頁面
		document.newPage();
		// 段落
		Paragraph paragraph = new Paragraph();
		// 設置標題
		paragraph = new Paragraph(title, this.firsetTitleFont);
		paragraph.setLeading(50);
		paragraph.setAlignment(Element.ALIGN_LEFT);
		document.add(paragraph);
	}

	/**
	 * Cast ISO string (2016-12-27T08:15:05.674+01:00) to UTC localDateTime
	 */
	private static String castIsoTime(final String isoString)
	{
		final String timeString = isoString.split("\\.")[0];
		final LocalDateTime dateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME);
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return dateTime.format(formatter);
	}

	private PdfPTable createTable(final Integer cellNumber, final int[] relativeWidths) throws DocumentException
	{
		final PdfPTable table = new PdfPTable(cellNumber);
		table.setWidths(relativeWidths);// 設置寬度比
		table.setWidthPercentage(100);// 設置總寬度
		table.setSpacingBefore(10);// 設置表格上面空白
		table.setSpacingAfter(10);// 設置表格下面空白

		return table;
	}

	public void sendReportToAdmin() throws Exception
	{
		sendReportToAdmin(LocalDate.now(ZoneOffset.UTC));
	}

	public void sendReportToAdmin(final LocalDate localDate) throws Exception
	{
		final String subject = "OAM Report " + localDate.minusDays(1).toString();
		final String from = this.sendFrom;
		final String to = this.adminMail;
		final String bcc = this.bccMail;

		this.timeIntervalStart = LocalDateTime.of(localDate.minusDays(1), LocalTime.of(0, 0));
		this.timeIntervalEnd = LocalDateTime.of(localDate, LocalTime.of(23, 0));

		this.msIntervalStart = DateTimeUtil.LocalDateTimeToMills(this.timeIntervalStart);
		this.msIntervalEnd = DateTimeUtil.LocalDateTimeToMills(this.timeIntervalEnd);

		final ByteArrayOutputStream byteArrayOutputStream = createPdf(localDate, msIntervalStart, msIntervalEnd);
		final InputStreamSource source = new ByteArrayResource(byteArrayOutputStream.toByteArray());

		this.mailService.sendMimeMessageWithAttachments(subject, from, to, bcc, source);
	}
}