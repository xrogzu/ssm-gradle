package com.hobo.app.mahout.recommendation.book;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookFilterGenderResult {
    private static final Logger logger = LoggerFactory.getLogger(BookFilterGenderResult.class);

    final static int NEIGHBORHOOD_NUM = 2;
    final static int RECOMMENDER_NUM = 3;

    public static void _main(String[] args) throws TasteException, IOException {
        DataModel dataModel = RecommendFactory.buildDataModel("classpath:mahout/book/rating.csv");
        RecommenderBuilder rb1 = BookEvaluator.userEuclidean(dataModel);
        RecommenderBuilder rb2 = BookEvaluator.itemEuclidean(dataModel);
        RecommenderBuilder rb3 = BookEvaluator.userEuclideanNoPref(dataModel);
        RecommenderBuilder rb4 = BookEvaluator.itemEuclideanNoPref(dataModel);
        
        long uid = 65;
        System.out.print("userEuclidean       =>");
        filterGender(uid, rb1, dataModel);
        System.out.print("itemEuclidean       =>");
        filterGender(uid, rb2, dataModel);
        System.out.print("userEuclideanNoPref =>");
        filterGender(uid, rb3, dataModel);
        System.out.print("itemEuclideanNoPref =>");
        filterGender(uid, rb4, dataModel);
    }

    /**
     * 对用户性别进行过滤
     */
    public static void filterGender(long uid, RecommenderBuilder recommenderBuilder, DataModel dataModel) throws TasteException, IOException {
        Set<Long> userids = getMale("classpath:mahout/book/user.csv");

        //计算男性用户打分过的图书
        Set<Long> bookids = new HashSet<Long>();
        for (long uids : userids) {
            LongPrimitiveIterator iter = dataModel.getItemIDsFromUser(uids).iterator();
            while (iter.hasNext()) {
                long bookid = iter.next();
                bookids.add(bookid);
            }
        }

        IDRescorer rescorer = new FilterRescorer(bookids);
        List<RecommendedItem> list = recommenderBuilder.buildRecommender(dataModel).recommend(uid, RECOMMENDER_NUM, rescorer);
        RecommendFactory.showItems(uid, list, false);
    }

    /**
     * 获得男性用户ID
     */
    public static Set<Long> getMale(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(ResourceUtils.getFile(file)));
        Set<Long> userids = new HashSet<Long>();
        String s = null;
        while ((s = br.readLine()) != null) {
            String[] cols = s.split(",");
            if (cols[1].equals("M")) {// 判断男性用户
                userids.add(Long.parseLong(cols[0]));
            }
        }
        br.close();
        return userids;
    }

}

/**
 * 对结果重计算
 */
class FilterRescorer implements IDRescorer {
    final private Set<Long> userids;

    public FilterRescorer(Set<Long> userids) {
        this.userids = userids;
    }

    @Override
    public double rescore(long id, double originalScore) {
        return isFiltered(id) ? Double.NaN : originalScore;
    }

    @Override
    public boolean isFiltered(long id) {
        return userids.contains(id);
    }
}
