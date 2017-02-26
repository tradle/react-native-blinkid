//
//  RNBlinkIDOverlayViewController.m
//  RNBlinkID
//
//  Created by Mark Vayngrib on 2/26/17.
//  Copyright © 2017 Tradle, Inc. All rights reserved.
//

#import "RNBlinkIDOverlayViewController.h"

@interface RNBlinkIDOverlayViewController ()

@end

@implementation RNBlinkIDOverlayViewController
- (void) viewDidLoad {
    [super viewDidLoad];
    if (self.customTooltipLabel) {
        self.idCardSubview.tooltipLabel.text = self.customTooltipLabel;
        self.idCardSubview.tooltipLabel.numberOfLines = 0;
        self.idCardSubview.tooltipLabel.lineBreakMode = NSLineBreakByWordWrapping;
    }
}

@end
