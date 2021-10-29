import pandas as pd
from scipy import stats


def main():
    df = pd.read_csv('Analysis/data2.csv', index_col=0, header=None)
    # print(df)
    res = stats.f_oneway(df.loc['With'], df.loc['Without'])
    t = stats.ttest_ind(df.loc['With'], df.loc['Without'], equal_var=False)

    print(pd.DataFrame([round(p, 4) for p in t.pvalue if p < .1]))


if __name__ == "__main__":
    main()