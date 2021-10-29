import pandas as pd
from scipy import stats
import csv


def main():
    df = pd.read_csv('Analysis/data.csv', index_col=0, header=None)
    # print(df)
    res = stats.f_oneway(df.loc['With'], df.loc['Without'])
    t = stats.ttest_ind(df.loc['With'], df.loc['Without'], equal_var=False)

    with open('Analysis/results.csv', 'w') as csvfile:
        resultswriter = csv.writer(csvfile, delimiter=',')
        resultswriter.writerow(['Question', 'Statistic', 'pval'])
        for i in range(len(t.pvalue)):
            prepend = ''
            if t.pvalue[i] < 0.1:
                prepend = "\\rowcolor{green!8}"
            resultswriter.writerow([prepend + ' ' + str(i+1), round(t.statistic[i], 3), round(t.pvalue[i], 3)])

if __name__ == "__main__":
    main()