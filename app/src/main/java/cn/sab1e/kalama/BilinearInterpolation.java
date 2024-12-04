package cn.sab1e.kalama;

public class BilinearInterpolation {

    // 对二维矩阵进行双线性插值
    public static float[][] bilinearInterpolate(float[][] matrix, int targetWidth, int targetHeight) {
        int sourceWidth = matrix[0].length;
        int sourceHeight = matrix.length;
        float[][] result = new float[targetHeight][targetWidth];

        float xRatio = (float) (sourceWidth - 1) / (targetWidth - 1);  // x轴的缩放比例
        float yRatio = (float) (sourceHeight - 1) / (targetHeight - 1); // y轴的缩放比例

        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                // 计算原始矩阵中的位置
                float x = j * xRatio;
                float y = i * yRatio;
                int x1 = (int) x;
                int y1 = (int) y;
                int x2 = Math.min(x1 + 1, sourceWidth - 1);
                int y2 = Math.min(y1 + 1, sourceHeight - 1);

                // 计算插值
                float dx = x - x1;
                float dy = y - y1;
                float resultValue = (1 - dx) * (1 - dy) * matrix[y1][x1] +
                        dx * (1 - dy) * matrix[y1][x2] +
                        (1 - dx) * dy * matrix[y2][x1] +
                        dx * dy * matrix[y2][x2];
                result[i][j] = resultValue;
            }
        }
        return result;
    }
}
