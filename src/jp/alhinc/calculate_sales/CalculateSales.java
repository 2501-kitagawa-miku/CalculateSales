package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	//商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String BRANCH_FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String BRANCH_FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_NOT_EXIST = "商品定義ファイルが存在しません";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String NOT_CONSECUTIVE_NUMBERS ="売上ファイル名が連番になっていません";
	private static final String EXCEEDED_MAXIMUM_AMOUNT ="合計金額が10桁を超えました";
	private static final String BRANCH_PATH_NOT_EXIST = "の支店コードが不正です";
	private static final String COMMODITY_PATH_NOT_EXIST = "の商品コードが不正です";
	private static final String SALES_INVALID_FORMAT = "のフォーマットが不正です";


	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//コマンドライン引数の存在チェック（エラー処理3）
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readBranchFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if(!readCommodityFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		for (int i = 0; i < files.length; i++) {

			//ファイルかディレクトリかの判定（エラー処理3）
			if(files[i].isFile() && files[i].getName().matches( "^\\d{8}+.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//ファイル名の連番チェック（エラー処理2-1）
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(files[i].getName().substring(0, 8));
			int latter = Integer.parseInt(files[i + 1].getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println(NOT_CONSECUTIVE_NUMBERS);
				return;
			}
		}

		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;
			List<String> sales = new ArrayList<>();

			try {
				/*File file = new File(args[0], rcdFiles.get(i).getName());
				 * FileReader fr = new FileReader(file);
				 * が丁寧な書き方。
				 */
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				String line;
				while((line = br.readLine()) != null) {
					sales.add(line);
				}

					//売上ファイルのフォーマットチェック（エラー処理2-4）
				if(sales.size() != 3) {
					System.out.println("<" + rcdFiles.get(i).getName() + ">" + SALES_INVALID_FORMAT);
					return;
				}

				//支店コードの存在チェック（エラー処理2-3）
				if (!branchNames.containsKey(sales.get(0))) {
					System.out.println("<" + rcdFiles.get(i).getName() + ">" + BRANCH_PATH_NOT_EXIST);
					return;
				}

				//商品コードの存在チェック
				if (!commodityNames.containsKey(sales.get(1))) {
					System.out.println("<" + rcdFiles.get(i).getName() + ">" + COMMODITY_PATH_NOT_EXIST);
					return;
				}

				//売上金額が数字であることの確認（エラー処理3）
				if(!sales.get(2).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(sales.get(2));
				Long branchSaleAmount = branchSales.get(sales.get(0)) + fileSale;
				Long commoditySaleAmount = commoditySales.get(sales.get(1)) + fileSale;

				//合計金額が10桁以内かチェック（エラー処理2-2）
				if(branchSaleAmount >= 10000000000L){
					System.out.println(EXCEEDED_MAXIMUM_AMOUNT);
					return;
				}

				if(commoditySaleAmount >= 10000000000L){
					System.out.println(EXCEEDED_MAXIMUM_AMOUNT);
					return;
				}

				branchSales.put(sales.get(0), branchSaleAmount);
				commoditySales.put(sales.get(1), commoditySaleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}


		// 支店別集計ファイル書き込み処理
		if(!writeBranchFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if(!writeCommodityFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readBranchFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//ファイルの存在チェック（エラー処理1-1）
			if(!file.exists()) {
				System.out.println(BRANCH_FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {

				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");

				//ファイルのフォーマットチェック（エラー処理1-2）
				if(items.length != 2 || (!items[0].matches("^\\d{3}"))) {
					System.out.println(BRANCH_FILE_INVALID_FORMAT);
					return false;
				}

				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 商品コードと商品名を保持するMap
	 * @param 商品コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readCommodityFile(String path, String fileName, Map<String, String> commodityNames, Map<String, Long> commoditySales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//ファイルの存在チェック（エラー処理1-1）
			if(!file.exists()) {
				System.out.println(COMMODITY_FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			while((line = br.readLine()) != null) {
				String[] items = line.split(",");
				if(items.length != 2 || (!items[0].matches("^[a-zA-Z0-9]+$"))) {
					System.out.println(COMMODITY_FILE_INVALID_FORMAT);
					return false;
				}

				commodityNames.put(items[0], items[1]);
				commoditySales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeBranchFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 商品別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeCommodityFile(String path, String fileName, Map<String, String> commodityNames, Map<String, Long> commoditySales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key :commodityNames.keySet()) {
				bw.write(key + "," + commodityNames.get(key) + "," + commoditySales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
